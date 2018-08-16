package com.jbm.game.plugin.code;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import com.jbm.game.plugin.util.Args;
import com.jbm.game.plugin.util.Args.Three;
import com.jbm.game.plugin.model.FieldModel;
import com.jbm.game.plugin.util.ExcelUtil;
import com.jbm.game.plugin.util.FileUtil;
import com.jbm.game.plugin.util.FreeMarkerUtil;
import com.jbm.game.plugin.util.StringUtil;
import com.jbm.game.plugin.util.TimeUtil;

/**
 * Mongodb entity代码生成插件 <br>
 * 暂时不支持泛型
 * @author JiangBangMing
 *
 * 2018年8月15日 下午9:06:59
 */

@Mojo(name="mongoEntity",defaultPhase=LifecyclePhase.CLEAN)
public class MongoEntityBuilder extends AbstractMojo{
	/**
	 * 项目根目录 可以在命令行中由-D参数传入
	 */
	@Parameter(required = true, readonly = true, defaultValue = "${project.basedir}")
	private File basedir;

	/**
	 * 项目资源目录
	 *
	 */
	@Parameter(required = true, readonly = true, defaultValue = "${project.build.sourceDirectory}")
	private File sourceDirectory;

	/**
	 * 项目测试资源目录
	 *
	 */
	@Parameter(required = true, readonly = true, defaultValue = "${project.build.testSourceDirectory}")
	private File testSourceDirectory;

	/**
	 * 项目资源
	 */
	@Parameter(required = true, readonly = true, defaultValue = "${project.build.resources}")
	private List<Resource> resources;

	/**
	 * 项目测试资源
	 */
	@Parameter(required = true, readonly = true, defaultValue = "${project.build.testResources}")
	private List<Resource> testResources;

	/** 实体类相对路径 */
	@Parameter(defaultValue = "com/jbm/game/model/mongo/hall/entity")
	private String entityPackage;

	/** 配置表路径 */
	@Parameter(defaultValue = "/com/jbm/game/model/mongo")
	private String configTablePath;
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<File> sourceFileList = new ArrayList<File>(); // excel表文件
		List<File> classFileList = new ArrayList<File>(); // 已存在的java类文件
		List<String> entityClassNames = new ArrayList<>(); // 已生成类实体类名称
		Map<Args.Two<String, String>, List<FieldModel>> builderEntityMap = new HashMap<>(); // 需要生成的entity类

		String entityPath = sourceDirectory.getPath()+ entityPackage;
		FileUtil.getFiles(entityPath, classFileList, "java", null);
		FileUtil.getFiles(configTablePath, sourceFileList, "xlsx", null);
		getLog().info(String.format("源文件路径:%s,源文件数量:%d", configTablePath,sourceFileList.size()));
		getLog().info(String.format("输出路径:%s", entityPath));
		if (classFileList != null) {
			entityClassNames = classFileList.stream()
					.map(file -> file.getName().substring(0, file.getName().indexOf("."))).collect(Collectors.toList());
		}

		// 获取excel表属性
		try {
			// excel文件迭代
			for (File file : sourceFileList) {
				List<String> sheetNames = ExcelUtil.getSheetNames(file.getPath());
				if (sheetNames == null) {
					continue;
				}
				// 表单迭代
				for (String sheetName : sheetNames) {
					String className = sheetNameToClassName(sheetName);
					if (entityClassNames.contains(className)) { // 配置已经生成
						continue;
					}
					Three<List<String>, List<String>, List<String>> metaDatas = ExcelUtil.getMetaData(file.getPath(),
							sheetName);
					List<FieldModel> fields = new ArrayList<>();
					// 元数据迭代
					for (int i = 0; i < metaDatas.a().size(); i++) {
						FieldModel fieldModel = new FieldModel(parseFieldType(metaDatas.b().get(i)),
								metaDatas.a().get(i), metaDatas.c().get(i));
						fields.add(fieldModel);
					}
					builderEntityMap.put(Args.of(sheetName, className), fields);
				}
			}
		} catch (Exception e) {
			getLog().error("获取entity错误", e);
		}

		// freemarker生成代码
		Set<Entry<Args.Two<String, String>, List<FieldModel>>> entrySet = builderEntityMap.entrySet();
		getLog().info("需要生成文件数:"+builderEntityMap.size());
		for (Entry<Args.Two<String, String>, List<FieldModel>> entry : entrySet) {
			Args.Two<String, String> key = entry.getKey();
			Map<String, Object> datas = new HashMap<>(); // 数据模型
			datas.put("date", TimeUtil.getDateFormat1()); // 日期
			datas.put("package", entityPackage.replaceAll("\\\\", ".").substring(1)); // 包路径
			datas.put("tableName", key.a()); // 表名
			datas.put("className", key.b()); // 类名
			datas.put("fieldObjects", entry.getValue());
			String classPath = entityPath + File.separatorChar + key.b() + ".java";
			getLog().info(String.format("生成Entity类:%s", classPath));
			FreeMarkerUtil.writeToFile("mongoEntity.ftl", datas, classPath, "/ftl");
		}
		
	}
	/**
	 * 表单名转换为类名
	 * 
	 * @author JiangZhiYong
	 * @QQ 359135103 2017年10月31日 上午11:33:09
	 * @param sheetName
	 * @return
	 */
	private String sheetNameToClassName(String sheetName) {
		if (sheetName.contains("_")) {
			String[] split = sheetName.split("_");
			StringBuffer sb = new StringBuffer();
			for (String str : split) {
				sb.append(StringUtil.upFirstChar(str));
			}
			return sb.toString();
		}
		return sheetName;
	}

	/**
	 * 解析属性类型
	 * 
	 * @author JiangZhiYong
	 * @QQ 359135103 2017年10月31日 下午1:56:05
	 * @param filedType
	 * @return
	 */
	private String parseFieldType(String filedType) {
		if (filedType.equalsIgnoreCase("int")) {
			return "int";
		} else if (filedType.equalsIgnoreCase("double")) {
			return "double";
		} else if (filedType.equalsIgnoreCase("float")) {
			return "float";
		} else if (filedType.equalsIgnoreCase("string")) {
			return "String";
		} else if (filedType.equalsIgnoreCase("array")) {
			return "List";
		} else if (filedType.equalsIgnoreCase("object")) {
			return "Object";
		}

		return filedType;
	}
}
