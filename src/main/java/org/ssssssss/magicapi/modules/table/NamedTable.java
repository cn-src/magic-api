package org.ssssssss.magicapi.modules.table;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.exception.MagicAPIException;
import org.ssssssss.magicapi.modules.BoundSql;
import org.ssssssss.magicapi.modules.SQLModule;
import org.ssssssss.script.annotation.Comment;

import java.util.*;
import java.util.stream.Collectors;

public class NamedTable {

	String tableName;

	SQLModule sqlModule;

	String primary;

	Map<String, Object> columns = new HashMap<>();

	List<String> fields = new ArrayList<>();

	Where where = new Where(this);

	public NamedTable(String tableName, SQLModule sqlModule) {
		this.tableName = tableName;
		this.sqlModule = sqlModule;
	}

	@Comment("设置主键名，update时使用")
	public NamedTable primary(String primary) {
		this.primary = primary;
		return this;
	}

	@Comment("拼接where")
	public Where where() {
		return where;
	}

	@Comment("设置单列的值")
	public NamedTable column(@Comment("列名") String key, @Comment("值") Object value) {
		this.columns.put(key, value);
		return this;
	}

	@Comment("设置查询的列，如`columns('a','b','c')`")
	public NamedTable columns(@Comment("各项列") String... columns) {
		if (columns != null) {
			for (String column : columns) {
				column(column);
			}
		}
		return this;
	}

	@Comment("设置查询的列，如`columns(['a','b','c'])`")
	public NamedTable columns(Collection<String> columns) {
		if (columns != null) {
			columns.stream().filter(StringUtils::isNotBlank).forEach(this.fields::add);
		}
		return this;
	}

	@Comment("设置查询的列，如`column('a')`")
	public NamedTable column(String column) {
		if (StringUtils.isNotBlank(column)) {
			this.fields.add(column);
		}
		return this;
	}

	private List<Map.Entry<String, Object>> filterNotBlanks() {
		return this.columns.entrySet().stream()
				.filter(it -> StringUtils.isNotBlank(Objects.toString(it.getValue(), "")))
				.collect(Collectors.toList());
	}

	@Comment("执行插入")
	public int insert() {
		return insert(null);
	}

	@Comment("执行插入")
	public int insert(@Comment("各项列和值") Map<String, Object> data) {
		if (data != null) {
			this.columns.putAll(data);
		}
		List<Map.Entry<String, Object>> entries = filterNotBlanks();
		if (entries.isEmpty()) {
			throw new MagicAPIException("参数不能为空");
		}
		StringBuilder builder = new StringBuilder();
		builder.append("insert into ");
		builder.append(tableName);
		builder.append("(");
		builder.append(StringUtils.join(entries.stream().map(Map.Entry::getKey).toArray(), ","));
		builder.append(") values (");
		builder.append(StringUtils.join(Collections.nCopies(entries.size(), "?"), ","));
		builder.append(")");
		return sqlModule.update(new BoundSql(builder.toString(), entries.stream().map(Map.Entry::getValue).collect(Collectors.toList()), sqlModule));
	}

	@Comment("保存到表中，当主键有值时则修改，否则插入")
	public int save() {
		return this.save(null);
	}

	@Comment("保存到表中，当主键有值时则修改，否则插入")
	public int save(@Comment("各项列和值") Map<String, Object> data) {
		if (StringUtils.isBlank(this.primary)) {
			throw new MagicAPIException("请设置主键");
		}
		if (this.columns.get(this.primary) != null || (data != null && data.get(this.primary) != null)) {
			return update(data);
		}
		return insert(data);
	}


	@Comment("执行`select`查询")
	public List<Map<String, Object>> select() {
		return sqlModule.select(buildSelect());
	}

	@Comment("执行`selectOne`查询")
	public Map<String, Object> selectOne() {
		return sqlModule.selectOne(buildSelect());
	}

	private BoundSql buildSelect() {
		StringBuilder builder = new StringBuilder();
		builder.append("select ");
		if (this.fields.isEmpty()) {
			builder.append("*");
		} else {
			builder.append(StringUtils.join(this.fields, ","));
		}
		builder.append(" from ").append(tableName);
		List<Object> params = new ArrayList<>();
		if (!where.isEmpty()) {
			builder.append(where.getSql());
			params.addAll(where.getParams());
		}
		return new BoundSql(builder.toString(), params, sqlModule);
	}

	@Comment("执行分页查询")
	public Object page() {
		return sqlModule.page(buildSelect());
	}

	@Comment("执行update语句")
	public int update() {
		return update(null);
	}

	@Comment("执行update语句")
	public int update(@Comment("各项列和值") Map<String, Object> data) {
		if (null != data) {
			this.columns.putAll(data);
		}
		Object primaryValue = null;
		if (StringUtils.isNotBlank(this.primary)) {
			primaryValue = this.columns.remove(this.primary);
		}
		List<Map.Entry<String, Object>> entries = filterNotBlanks();
		if (entries.isEmpty()) {
			throw new MagicAPIException("要修改的列不能为空");
		}
		StringBuilder builder = new StringBuilder();
		builder.append("update ");
		builder.append(tableName);
		builder.append(" set ");
		List<Object> params = new ArrayList<>();
		for (int i = 0,size = entries.size(); i < size; i++) {
			Map.Entry<String, Object> entry = entries.get(i);
			builder.append(entry.getKey()).append(" = ?");
			params.add(entry.getValue());
			if(i + 1 < size){
				builder.append(",");
			}
		}
		if (!where.isEmpty()) {
			builder.append(where.getSql());
			params.addAll(where.getParams());
		} else if(primaryValue != null){
			builder.append(" where ").append(this.primary).append(" = ?");
			params.add(primaryValue);
		}else{
			throw new MagicAPIException("主键值不能为空");
		}
		return sqlModule.update(new BoundSql(builder.toString(), params, sqlModule));
	}
}