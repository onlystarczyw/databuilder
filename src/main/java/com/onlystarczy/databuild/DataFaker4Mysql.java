package com.onlystarczy.databuild;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.onlystarczy.databuild.mysql.DataStructure4Mysql;
import com.onlystarczy.databuild.utils.CollectionUtils;
import com.onlystarczy.databuild.utils.DateUtils;

/**
 * 构造mysql测试数据
 * @author zycheng6
 *
 */
public class DataFaker4Mysql {

	private static List<String> TYPE_INTEGER = Lists.newArrayList("tinyint", "smallint", "mediumint", "int", "bigint");

	private static List<String> TYPE_STRING = Lists.newArrayList("char", "varchar", "text", "blob", "enum", "set");

	private static List<String> TYPE_DATE = Lists.newArrayList("datetime", "timestamp", "date", "time", "year");

	private static List<String> TYPE_BOOLEAN = Lists.newArrayList("bit");

	/**
	 * <pre>
	 *   `DataColumn1` varchar(60) DEFAULT NULL COMMENT 'xxxx', SQL片段
	 *   $1  `DataColumn1` 列字段名称
	 *   $2  varchar 列字段类型
	 *   $3  60      列字段长度
	 * </pre>
	 */
	private static Pattern DataColumn_PATTERN1 = Pattern.compile("^(`.*?`)[ |	]+([a-z]+)\\((\\d+)\\)");

	/**
	 * <pre>
	 * `DataColumn2` timestamp NULL DEFAULT NULL COMMENT 'xx时间', SQL片段
	 *  $1 `DataColumn2`
	 *  $2 timestamp
	 * </pre>
	 */
	private static Pattern DataColumn_PATTERN2 = Pattern.compile("^(`.*?`)[ |	]+([a-z]+)");

	/**
	 * CREATE TABLE `table` ( SQL片段 $1 `table` 表名
	 * 
	 */
	private static Pattern TABLE_PATTERN = Pattern.compile("^create[ |	]+table[ |	]+(.*?)\\(");

	/**
	 * <pre>
	 * 初始化数据分隔符默认一个空格
	 * 
	 * `name` 曹操 刘备 孙坚 孙策 孙权 张飞 关羽 赵云
	 * `code` 001 002 003 004
	 * 
	 * <pre>
	 */
	private static String INIT_DATA_DATACOLUMN_SPLIT = " ";
	
	/**
	 * <pre>
	 * true : INSERT INTO (`字段1`, `字段2`) `表名` VALUES (...)
	 * false : INSERT INTO `表名` VALUES (...)
	 * 
	 * <pre>
	 */
	private static boolean CONTACT_TABLE_FIELDS = true;

	/**
	 * yyyy-MM-dd HH:mm:ss~yyyy-MM-dd HH:mm:ss 中间的分隔符
	 */
	private static String TEMPLATE_DATA_DATE_SPLIT = "~";

	/**
	 * 模板日期格式
	 */
	private static String TEMPLATE_DATA_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/**
	 * 模板时间格式正则
	 */
	private static Pattern TEMPLATE_DATA_DATE_PATTERN = Pattern
			.compile(TEMPLATE_DATA_DATE_FORMAT.replaceAll("y|M|d|H|m|s", "\\\\d") + TEMPLATE_DATA_DATE_SPLIT
					+ TEMPLATE_DATA_DATE_FORMAT.replaceAll("y|M|d|H|m|s", "\\\\d"));
	
	/**
	 * 线程池数量
	 */
	private static int THREAD_SIZE = 20;
	
	private static String charSet = "UTF-8";
	
	/**
	 * 每批次线程任务数量
	 */
	private static int BATCH_SIZE = 10000;
	
	/**
	 * 线程池
	 */
	private static ExecutorService EXECUTORSERVICE = Executors.newFixedThreadPool(THREAD_SIZE);
	
	/**
	 * 文件sql部分 开始关键词
	 */
	private static String SQL_TAG = "##建表语句##";
	
	/**
	 * 文件数据量部分 开始关键词
	 */
	private static String DATA_SIZE_TAG = "##数据量##";
	
	/**
	 * 文件初始化数据部分 开始关键词
	 */
	private static String INIT_DATA_TAG = "##基础数据##";
	
	
	private static String GEN_SQL_PATH = File.separator + "sql_" + System.currentTimeMillis() + File.separator;

	public static void main(String[] args) {

		
//		String path = "E:\\jxrt_sql\\";
//		String path = "E:\\jxrt_sql\\test";

		String path = loadInput();
		

		try {
			System.out.println("任务开始。。。。。");
			
			File folder = new File(path);
			
			if(!folder.isDirectory()) {
				System.out.println("请输入正确的文件夹");
				return;
			}
			
			File[] listFiles = folder.listFiles();
			
			for (File templateFile : listFiles) {
				if(templateFile.isFile()) {
					System.out.println(templateFile.getName() + " 任务开始执行");
					try {
						execSingleTableFile(path, templateFile);
					}catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println(templateFile.getName() + " 任务执行结束 ~");
				}
			}
			

			System.out.println("任务完成。。。。。");
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			EXECUTORSERVICE.shutdown();
		}

	}

	
	private static void execSingleTableFile(String path, File templateFile) throws IOException {
		
		TepmlateFileAttr attr = extractSqlAndInitData(templateFile);

		DataStructure4Mysql ds = extractCreateSql(path, attr.createSqlLines);

		Map<String, Object[]> param = extractData(attr.templateDataLines, ds.getColumns());
		
		int dataSize = attr.dataSize;
		
		AtomicInteger atomic = new AtomicInteger(0);
		
		List<Future<?>> list = Lists.newArrayList();
		
		for (int i = 0; i < (dataSize / BATCH_SIZE) + 1; i++) {
			
			AtomicInteger i1 = new AtomicInteger(i);
			
			Future<?> submit = EXECUTORSERVICE.submit(new Runnable() {
				@Override
				public void run() {
					int size = BATCH_SIZE;
					if(i1.get() == (dataSize / BATCH_SIZE)) {
						size = dataSize % BATCH_SIZE;
					}
					List<String> result = Lists.newArrayList();
					for (int j = 0; j < size; j++) {
						List<Object> objs = genDataColumnData(ds.getColumns(), param);
						String sql = baseCreateSql(ds.getName(), ds.getColumns(), objs);
						result.add(sql);
						int andAdd = atomic.addAndGet(1);
						if (andAdd > 0 && 0 == andAdd % 10000) {
							System.out.println(ds.getName() + " 已生成" + andAdd + "条数据");
						}
						if(andAdd == dataSize) {
							System.out.println(ds.getName() + " 已生成" + dataSize + "条数据");
						}
					}
					try {
						write(path, ds, result);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			
			list.add(submit);
			
		}


		for (Future<?> future : list) {
			try {
				future.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 将文件中的建表语句、数据量、基础数据分开
	 * @param file
	 * @return
	 * @throws IOException 
	 */
	private static TepmlateFileAttr extractSqlAndInitData(File file) throws IOException {
		TepmlateFileAttr attr = new TepmlateFileAttr();
		
		List<String> lines = FileUtils.readLines(file, Charset.forName(charSet));

		String tagStr = "";
		for (String line : lines) {
			if(StringUtils.isBlank(line)) {
				continue;
			}
			//TODO
//			System.out.println(line);
			if(line.contains(SQL_TAG) || line.contains(DATA_SIZE_TAG) || line.contains(INIT_DATA_TAG)) {
				//TODO
//				System.out.println("tagStr = line.trim()");
				tagStr = line.trim();
				continue;
			}
			if(StringUtils.isBlank(SQL_TAG)) {
				continue;
			}
			if(Objects.equals(tagStr, SQL_TAG)) {
				attr.createSqlLines.add(line);
			}
			if(Objects.equals(tagStr, DATA_SIZE_TAG)) {
				try {
					attr.dataSize = Integer.parseInt(line.trim());
				}catch (Exception e) {
					throw new RuntimeException("文件 " + file.getName() + " 数据量" + tagStr + "格式错误");
				}
			}
			if(Objects.equals(tagStr, INIT_DATA_TAG)) {
				attr.templateDataLines.add(line);
			}
		}
		
		return attr;
	}

	private static synchronized void write(String path, DataStructure4Mysql ds, List<String> result)
			throws IOException {
		FileUtils.writeLines(new File(Paths.get(path + GEN_SQL_PATH, ds.getName().replace("`", "") + ".sql").toString()), result,
				true);
	}

	/**
	 * 提取建表语句
	 * @param path
	 * @param lines
	 * @return
	 * @throws IOException
	 */
	private static DataStructure4Mysql extractCreateSql(String path, List<String> lines) throws IOException {

		DataStructure4Mysql ds = new DataStructure4Mysql();

		List<DataColumn> columns = Lists.newArrayList();

		for (String line : lines) {
			if (StringUtils.isBlank(ds.getName())) {
				ds.setName(extractTableName(line));
				continue;
			}

			DataColumn column = extractDataColumn(line);
			if (Objects.isNull(column)) {
				continue;
			}
			
			if(line.toUpperCase().contains("AUTO_INCREMENT")) {
				// 自增字段跳过不处理, 并却需要开启CONTACT_TABLE_FIELDS=true字段
				if(!CONTACT_TABLE_FIELDS) {
					throw new RuntimeException("存在自增字段 "+ column.getName() +" , 请开启 CONTACT_TABLE_FIELD 配置");
				}
				continue;
			}
			
			columns.add(column);
		}

		ds.setColumns(columns);
		return ds;
	}

	/**
	 * 命令行输入
	 * @return
	 */
	private static String loadInput() {

		String path = null;

		Scanner scan = new Scanner(System.in);

		System.out.println("请输入工作路径 : ");

		while (scan.hasNext()) {
			
			 String nextLine = scan.nextLine();

			if(StringUtils.isBlank(nextLine)) {
				continue;
			}
			File file = new File(nextLine);
			if(!file.isDirectory()) {
				System.out.println("请输入正确的工作路径");
				continue;
			}
			path = nextLine;
			break;
		}

		scan.close();
		
		return path;
	}

	/**
	 * 生成列数据
	 * @param DataColumns
	 * @param param
	 * @return
	 */
	private static List<Object> genDataColumnData(List<DataColumn> DataColumns, Map<String, Object[]> param) {
		List<Object> objs = Lists.newArrayList();

		for (DataColumn column : DataColumns) {
			String type = column.getType().toLowerCase();
			String name = column.getName().toLowerCase();
			int length = column.getLength();
			if (TYPE_INTEGER.contains(type)) {
				objs.add(param.containsKey(name) ? random(param.get(name))
						: random(new Object[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
				continue;
			}

			if (TYPE_STRING.contains(type)) {
				if (length > 32) {
					objs.add(param.containsKey(name) ? random(param.get(name)) : UUID.randomUUID().toString());
				} else if(length == 0) {
					//TEXT LONGTEXT
					objs.add(param.containsKey(name) ? random(param.get(name))
							: UUID.randomUUID().toString() + UUID.randomUUID().toString());
				}else {
					objs.add(param.containsKey(name) ? random(param.get(name))
							: UUID.randomUUID().toString().subSequence(0, length - 1));
				}
				continue;
			}

			if (TYPE_DATE.contains(type)) {
				if (param.containsKey(name)) {
					RandomDate RandomDate = (RandomDate) random(param.get(name));
					Date apply = RandomDate.apply(RandomDate.getDateStr());
					objs.add(apply);
				} else {
					objs.add(new Date());
				}
				continue;
			}

			if (TYPE_BOOLEAN.contains(type)) {
				objs.add(random(new Object[] { 0, 1 }));
				continue;
			}

		}
		return objs;
	}

	/**
	 * 提取元数据
	 * @param datas
	 * @param DataColumns
	 * @return
	 */
	private static Map<String, Object[]> extractData(List<String> datas, List<DataColumn> DataColumns) {
		if (CollectionUtils.isEmpty(datas)) {
			System.out.println("未检测到该表的基础数据");
			return Maps.newHashMap();
		}
		HashMap<String, Object[]> resultMap = Maps.newHashMap();

		Map<String, DataColumn> DataColumnMap = DataColumns.stream()
				.collect(Collectors.toMap(c -> c.getName(), c -> c));

		for (String data : datas) {
			if (StringUtils.isBlank(data)) {
				continue;
			}
			data = data.trim();
			String[] split2 = data.split(INIT_DATA_DATACOLUMN_SPLIT);
			String DataColumnKey = split2[0];
			DataColumnKey = "`" + DataColumnKey.replace("`", "") + "`";

			if (!DataColumnMap.containsKey(DataColumnKey)) {
				continue;
			}

			DataColumn column = DataColumnMap.get(DataColumnKey);

			List<Object> dataColumnInitDatas = Lists.newArrayList();
			for (int i = 1; i < split2.length; i++) {
				if (StringUtils.isBlank(split2[i])) {
					continue;
				}
				try {
					if (TYPE_INTEGER.contains(column.getType())) {
						dataColumnInitDatas.add(Integer.parseInt(split2[i]));
					} else if (TYPE_STRING.contains(column.getType())) {
						dataColumnInitDatas.add(split2[i]);
					} else if (TYPE_DATE.contains(column.getType())) {
//						DateUtils.randomTime(DataColumnKey, data);
//						dataColumnInitDatas.add(new RandomDate().apply(split2[i]));
					} else {
//						dataColumnInitDatas.add(split2[i]);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (TYPE_DATE.contains(column.getType())) {
				Matcher matcher = TEMPLATE_DATA_DATE_PATTERN.matcher(data);
				while (matcher.find()) {
					dataColumnInitDatas.add(new RandomDate(matcher.group(0)));
				}
			}

			if (CollectionUtils.isNotEmpty(dataColumnInitDatas)) {
				resultMap.put(DataColumnKey, dataColumnInitDatas.toArray());
			}
		}
		return resultMap;
	}

	/**
	 * 提取表字段名称
	 * @param line
	 * @return
	 */
	private static DataColumn extractDataColumn(String line) {
		if (StringUtils.isBlank(line)) {
			return null;
		}
		line = line.trim();

		Matcher matcher = DataColumn_PATTERN1.matcher(line);
		while (matcher.find()) {
			String name = matcher.group(1);
			String type = matcher.group(2);
			String le = matcher.group(3);
			return new DataColumn(name, type, Integer.parseInt(le));
		}

		Matcher matcher1 = DataColumn_PATTERN2.matcher(line);
		while (matcher1.find()) {
			String name = matcher1.group(1);
			String type = matcher1.group(2);
			return new DataColumn(name, type);
		}

		return null;
	}

	/**
	 * 提取表名
	 * @param line
	 * @return
	 */
	private static String extractTableName(String line) {
		if (StringUtils.isBlank(line)) {
			return null;
		}
		line = line.trim().toLowerCase();

		Matcher matcher = TABLE_PATTERN.matcher(line);
		while (matcher.find()) {
			String name = matcher.group(1);
			return name.trim();
		}

		return null;
	}

	/**
	 * 构建insert语句
	 * @param tableName
	 * @param columns
	 * @param objs
	 * @return
	 */
	private static String baseCreateSql(String tableName, List<DataColumn> columns, List<Object> objs) {

		List<Object> result = Lists.newArrayList();

		for (Object d : objs) {
			if (d instanceof Integer) {
				result.add(d);
			} else if (d instanceof String) {
				result.add(String.format("'%s'", d));
			} else if (d instanceof Date) {
				result.add(String.format("'%s'", DateUtils.formatByDateTimeFormatter((Date) d)));
			}
		}
		
		String sql = null;
		if(CONTACT_TABLE_FIELDS) {
			sql = "INSERT INTO %s (%s) VALUES (%s);";
			List<String> fields = columns.stream().map(DataColumn::getName).collect(Collectors.toList());
			return String.format(sql, tableName,  StringUtils.join(fields, ","),  StringUtils.join(result, ","));
		}else {
			sql = "INSERT INTO %s VALUES (%s);";
			return String.format(sql, tableName, StringUtils.join(result, ","));
		}

	}

	/**
	 * 随机抽取该数组中的任意一个元素
	 * @param params
	 * @return
	 */
	public static Object random(Object[] params) {
		int i = (int) (Math.random() * (params.length));
		return params[i];
	}

	public static class RandomDate implements Function<String, Date> {

		private String dateStr;

		public RandomDate(String dateStr) {
			this.dateStr = dateStr;
		}

		public RandomDate() {
		}

		public String getDateStr() {
			return dateStr;
		}

		public void setDateStr(String dateStr) {
			this.dateStr = dateStr;
		}

		@Override
		public Date apply(String t) {
			if (StringUtils.isBlank(t)) {
				return new Date();
			}
			String[] split = t.split(TEMPLATE_DATA_DATE_SPLIT);
			Date randomTime = DateUtils.randomTime(split[0].trim(), split[1].trim(), TEMPLATE_DATA_DATE_FORMAT);
			return randomTime;
		}

	}
	
	public static class TepmlateFileAttr {
		
		public List<String> createSqlLines = Lists.newArrayList();
		public List<String> templateDataLines = Lists.newArrayList();
		public int dataSize = 0;
		
	}

}
