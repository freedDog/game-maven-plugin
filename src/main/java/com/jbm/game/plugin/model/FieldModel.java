package com.jbm.game.plugin.model;

import com.jbm.game.plugin.util.StringUtil;

/**
 * 对象模型
 * @author JiangBangMing
 *
 * 2018年8月15日 下午9:00:05
 */
public class FieldModel {
	/** 属性类型 */
	private String fieldType;
	/** 属性名称 */
	private String fieldName;
	/** 属性名称首字母大写 */
	private String fieldNameUpFirst;
	/** 描述 */
	private String description;
	

	public FieldModel(String fieldType, String fieldName, String description) {
		super();
		this.fieldType = fieldType;
		this.fieldName = fieldName;
		this.description = description;
		this.fieldNameUpFirst=StringUtil.upFirstChar(fieldName);
	}

	public String getFieldType() {
		return fieldType;
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFieldNameUpFirst() {
		return fieldNameUpFirst;
	}

	public void setFieldNameUpFirst(String fieldNameUpFirst) {
		this.fieldNameUpFirst = fieldNameUpFirst;
	}
}
