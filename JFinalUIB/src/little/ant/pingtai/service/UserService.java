package little.ant.pingtai.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import little.ant.pingtai.common.DictKeys;
import little.ant.pingtai.common.SplitPage;
import little.ant.pingtai.model.DepartmentModel;
import little.ant.pingtai.model.UserModel;
import little.ant.pingtai.model.UserInfoModel;
import little.ant.pingtai.thread.ThreadParamInit;
import little.ant.pingtai.tools.ToolSecurityPbkdf2;
import little.ant.pingtai.tools.ToolSqlXml;

import org.apache.log4j.Logger;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;

public class UserService extends BaseService {

	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(UserService.class);

	/**
	 * 保存
	 * 
	 * @param user
	 * @param passWord
	 * @param userInfo
	 */
	@Before(Tx.class)
	public void save(UserModel user, String password, UserInfoModel userInfo) {
		try {
			// 密码加密
			byte[] salt = ToolSecurityPbkdf2.generateSalt();// 密码盐
			byte[] encryptedPassword = ToolSecurityPbkdf2.getEncryptedPassword(password, salt);
			user.set("salt", salt);
			user.set("password", encryptedPassword);

			// 保存用户信息
			userInfo.save();

			// 保存用户
			user.set("userinfoids", userInfo.getStr("ids"));
			user.set("errorcount", 0);
			user.set("status", "1");
			user.save();

			// 缓存
			user = UserModel.dao.findById(user.getStr("ids"));
			CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + user.getStr("ids"), user);
			CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + user.getStr("username"), user);
			CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + userInfo.getStr("email"), user);
			CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + userInfo.getStr("mobile"), user);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("保存用户密码加密操作异常");
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException("保存用户密码加密操作异常");
		} catch (Exception e) {
			throw new RuntimeException("保存用户异常");
		}
	}

	/**
	 * 更新
	 * 
	 * @param user
	 * @param passWord
	 * @param userInfo
	 */
	@Before(Tx.class)
	public void update(UserModel user, String password, UserInfoModel userInfo) {
		try {
			// 密码加密
			if (null != password && !password.trim().equals("")) {
				UserModel oldUser = UserModel.dao.findById(user.getStr("ids"));
				byte[] salt = oldUser.getBytes("salt");// 密码盐
				byte[] encryptedPassword = ToolSecurityPbkdf2.getEncryptedPassword(password, salt);
				user.set("password", encryptedPassword);
			}

			// 更新用户
			user.update();
			userInfo.update();

			// 缓存
			user = UserModel.dao.findById(user.getStr("ids"));
			CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + user.getStr("ids"), user);
			CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + user.getStr("username"), user);
			CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + userInfo.getStr("email"), user);
			CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + userInfo.getStr("mobile"), user);
		} catch (Exception e) {
			throw new RuntimeException("更新用户异常");
		}
	}

	/**
	 * 删除
	 * 
	 * @param userIds
	 */
	@Before(Tx.class)
	public void delete(String userIds) {
		UserModel user = UserModel.dao.findById(userIds);
		String userInfoIds = user.getStr("userinfoids");
		UserInfoModel userInfo = UserInfoModel.dao.findById(userInfoIds);

		// 缓存
		CacheKit.remove(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + user.getStr("ids"));
		CacheKit.remove(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + user.getStr("username"));
		CacheKit.remove(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + userInfo.getStr("email"));
		CacheKit.remove(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + userInfo.getStr("mobile"));

		// 删除
		user.delete();
		UserInfoModel.dao.deleteById(userInfoIds);
	}

	/**
	 * 设置用户所在的组
	 * 
	 * @param userIds
	 * @param groupIds
	 */
	public void setGroup(String userIds, String groupIds) {
		UserModel user = UserModel.dao.findById(userIds);
		String userInfoIds = user.getStr("userinfoids");
		UserInfoModel userInfo = UserInfoModel.dao.findById(userInfoIds);

		user.set("groupids", groupIds).update();

		// 缓存
		user = UserModel.dao.findById(user.getStr("ids"));
		CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + user.getStr("ids"), user);
		CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + user.getStr("username"), user);
		CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + userInfo.getStr("email"), user);
		CacheKit.put(DictKeys.cache_name_system, ThreadParamInit.cacheStart_user + userInfo.getStr("mobile"), user);
	}

	/**
	 * 获取子节点数据
	 * @param deptIds
	 * @return
	 */
	public String childNodeData(String deptIds) {
		// 查询部门数据
		List<DepartmentModel> deptList = null;
		if (null != deptIds) {
			String sql = ToolSqlXml.getSql("pingtai.department.childNode");
			deptList = DepartmentModel.dao.find(sql, deptIds.replace("dept_", ""));
		} else {
			String sql = ToolSqlXml.getSql("pingtai.department.rootNode");
			deptList = DepartmentModel.dao.find(sql);
		}

		// 查询用户数据
		List<UserModel> userList = null;
		if (null != deptIds) {
			String sql = ToolSqlXml.getSql("pingtai.user.treeUserNode");
			userList = UserModel.dao.find(sql, deptIds.replace("dept_", ""));
		}

		StringBuilder sb = new StringBuilder();
		sb.append("[");

		// 封装用户数据
		if (null != userList) {
			int userSize = userList.size() - 1;
			for (UserModel user : userList) {
				sb.append(" { ");
				sb.append(" id : '").append("user_").append(user.getStr("ids")).append("', ");
				sb.append(" name : '").append(user.getStr("names")).append("', ");
				sb.append(" isParent : false, ");
				sb.append(" font : {'font-weight':'bold'}, ");
				sb.append(" icon : '/jsFile/zTree/css/zTreeStyle/img/diy/5.png' ");
				sb.append(" }");
				if (userList.indexOf(user) < userSize) {
					sb.append(", ");
				}
			}
		}

		int size = deptList.size() - 1;
		if (null != userList && userList.size() != 0 && size >= 0) {
			sb.append(", ");
		}

		// 封装部门数据
		for (DepartmentModel dept : deptList) {
			sb.append(" { ");
			sb.append(" id : '").append("dept_").append(dept.getPrimaryKeyValue()).append("', ");
			sb.append(" name : '").append(dept.get("names")).append("', ");

			if (null != deptIds) {
				sb.append(" isParent : true, ");
			} else {
				sb.append(" isParent : ").append(dept.getStr("isparent")).append(", ");
			}

			sb.append(" font : {'font-weight':'bold'}, ");
			sb.append(" icon : '/jsFile/zTree/css/zTreeStyle/img/diy/").append(dept.getStr("images")).append("' ");
			sb.append(" }");
			if (deptList.indexOf(dept) < size) {
				sb.append(", ");
			}
		}

		sb.append("]");

		return sb.toString();
	}

	/**
	 * 分页
	 * 
	 * @param splitPage
	 */
	public void list(SplitPage splitPage) {
		String select = " select u.ids, u.username, ui.names, ui.email, ui.mobile, ui.birthday, d.names as deptnames ";
		splitPageBase(splitPage, select, "pingtai.user.splitPage");
	}
	
}
