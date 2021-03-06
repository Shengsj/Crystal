package sAlg;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import sData.IntDegreeArray;
import sData.OneNodeKey;
import sData.TwoNodeKey;
import sData.TwoNodeKeyIntDegreeArray;
import sDeprecated.BowlO.OneNodeKeyIntDegreeArray;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * <h1>
 * Count House
 * </h1>
 * <ul>
 * 	<li>Pattern name: House </li>
 * 	<li>Pattern: (0,1),(0,3),(0,4),(1,2),(2,3),(3,4)  </li>
 * 	<li>Core: (0,1,3)</li>
 * 	<li>Non Core Node: [2,4]</li>
 * </ul>
 * @author zhanghao
 */

public class House extends Configured implements Tool {

    private static Logger log = Logger.getLogger(House.class);

    public static class SolarParitionKey implements WritableComparable<SolarParitionKey> {

	public int key;

	public SolarParitionKey() {
	    key = 0;
	}

	public SolarParitionKey(int key) {
	    this.key = key;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
	    key = in.readInt();

	}

	@Override
	public void write(DataOutput out) throws IOException {
	    out.writeInt(key);

	}

	@Override
	public int compareTo(SolarParitionKey other) {
	    if (key > other.key) {
		return 1;
	    }
	    if (key < other.key) {
		return -1;
	    }

	    return 0;
	}

	@Override
	public int hashCode() {
	    return key;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    SolarParitionKey other = (SolarParitionKey) obj;
		return key == other.key;
	}

    }

    public static String appendNumberToLengthThree(Integer i) {
	if (i > 0) {
	    if (i < 10) {
		return "00" + i;
	    } else if (i < 100) {
		return "0" + i;
	    } else {
		return i.toString();
	    }
	} else {
	    return "000";
	}
    }

    public static class HouseMPreprocessMapper1
	    extends Mapper<OneNodeKey, IntDegreeArray, SolarParitionKey, OneNodeKeyIntDegreeArray> {
	private int p = 240;
	private final SolarParitionKey aKey = new SolarParitionKey();

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
	    p = 240;
	}

	@Override
	protected void map(OneNodeKey key, IntDegreeArray value, Context context)
		throws IOException, InterruptedException {
	    aKey.key = key.node % p;
	    context.write(aKey, new OneNodeKeyIntDegreeArray(key, value));
	}
    }

    public static class HouseMPreprocessReducer1
	    extends Reducer<SolarParitionKey, OneNodeKeyIntDegreeArray, NullWritable, NullWritable> {
	@Override
	protected void reduce(SolarParitionKey key, Iterable<OneNodeKeyIntDegreeArray> values, Context context)
		throws IOException, InterruptedException {
	    Configuration conf = context.getConfiguration();
	    String path = conf.get("test.triangleIntermidiate1");
	    String outputPath = path + "-" + key.key;
	    FileSystem fileSystem = null;

	    fileSystem = FileSystem.get(conf);

	    // 定义输出流（SequenceFile）

	    SequenceFile.Writer writer = SequenceFile.createWriter(fileSystem, conf, new Path(outputPath),
		    OneNodeKey.class, IntDegreeArray.class);
	    for (OneNodeKeyIntDegreeArray onenodeArray : values) {
		OneNodeKeyIntDegreeArray outputResult = WritableUtils.clone(onenodeArray, context.getConfiguration());
		writer.append(outputResult.key, outputResult.array);
	    }
	    IOUtils.closeStream(writer);
	}
    }

    public static class HouseMPreprocessMapper2
	    extends Mapper<TwoNodeKey, IntDegreeArray, SolarParitionKey, TwoNodeKeyIntDegreeArray> {
	private int p = 240;
	private final SolarParitionKey aKey = new SolarParitionKey();

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
	    p = 240;
	}

	@Override
	protected void map(TwoNodeKey key, IntDegreeArray value, Context context)
		throws IOException, InterruptedException {
	    aKey.key = key.node1 % p;
	    context.write(aKey, new TwoNodeKeyIntDegreeArray(key, value));

	    // aKey.key = key.node2 % p;
	    // TwoNodeKey newKey = new TwoNodeKey(key.node2, key.node1,
	    // key.node2Degree, key.node1Degree);
	    // context.write(aKey, new TwoNodeKeyIntDegreeArray(newKey, value));
	}
    }

    public static class HouseMPreprocessReducer2
	    extends Reducer<SolarParitionKey, TwoNodeKeyIntDegreeArray, NullWritable, NullWritable> {
	@Override
	protected void reduce(SolarParitionKey key, Iterable<TwoNodeKeyIntDegreeArray> values, Context context)
		throws IOException, InterruptedException {
	    Configuration conf = context.getConfiguration();
	    String path = conf.get("test.triangleIntermidiate2");
	    String outputPath = path + "-" + key.key;
	    FileSystem fileSystem = null;

	    fileSystem = FileSystem.get(conf);

	    // 定义输出流（SequenceFile）

	    SequenceFile.Writer writer = SequenceFile.createWriter(fileSystem, conf, new Path(outputPath),
		    TwoNodeKey.class, IntDegreeArray.class);
	    for (TwoNodeKeyIntDegreeArray onenodeArray : values) {
		TwoNodeKeyIntDegreeArray outputResult = WritableUtils.clone(onenodeArray, context.getConfiguration());
		writer.append(outputResult.key, outputResult.array);
	    }
	    IOUtils.closeStream(writer);
	}
    }

    public static class HouseMMapper extends Mapper<Object, Text, NullWritable, NullWritable> {
	private Integer p = new Integer(1);
	private Integer offset = new Integer(0);

	private final TIntObjectHashMap<TIntArrayList> map = new TIntObjectHashMap<TIntArrayList>();

	private List<Integer> getProblem(Text line) {
	    StringTokenizer st = new StringTokenizer(line.toString());

	    List<Integer> _problem = new ArrayList<Integer>();
	    try {
		for (;;) {
		    _problem.add(Integer.parseInt(st.nextToken()));
		}
	    } catch (Exception localException) {
		return _problem;
	    }
	}

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {

	    Configuration conf = context.getConfiguration();
	    Integer NIBFp = conf.getInt("test.p", 1);
	    Integer NIBFoffset = conf.getInt("test.offset", 0);

	    Path[] allPaths = org.apache.hadoop.mapreduce.filecache.DistributedCache.getLocalCacheFiles(context.getConfiguration());

	    p = NIBFp;
	    offset = NIBFoffset;

	    SequenceFile.Reader reader = null;

	    ArrayList<Integer> shuffleList = new ArrayList<Integer>();
	    for (int i = 0; i < 240; ++i) {

		if ((i % p) == offset) {
		    shuffleList.add(i);
		}
	    }

	    Collections.shuffle(shuffleList, new Random(System.currentTimeMillis()));

	    for (int k = 0; k < shuffleList.size(); k++) {
		reader = new SequenceFile.Reader(FileSystem.getLocal(conf), allPaths[shuffleList.get(k)], conf);
		OneNodeKey key = (OneNodeKey) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
		IntDegreeArray value = (IntDegreeArray) ReflectionUtils.newInstance(reader.getValueClass(), conf);

		long position = reader.getPosition();
		while (reader.next(key, value)) {
		    String syncSeen = reader.syncSeen() ? "*" : "";
		    position = reader.getPosition(); // beginning of next record
		    int theCopiedKey = key.getNode();

		    for (int h = 0; h < value.size; ++h) {
			int keyToAddMap = value.getNode(h);
			if (map.containsKey(keyToAddMap)) {
			    map.get(keyToAddMap).add(theCopiedKey);
			} else {
			    TIntArrayList vi = new TIntArrayList();
			    vi.add(theCopiedKey);
			    map.put(keyToAddMap, vi);
			}
		    }
		}
		IOUtils.closeStream(reader);
	    }
	}

	private void readEdge(String path, Context context, int i, TIntObjectHashMap<TIntArrayList> edgeList)
		throws IOException {
	    Configuration conf = context.getConfiguration();

	    SequenceFile.Reader reader = null;
	    FileSystem fs = FileSystem.get(conf);
	    reader = new SequenceFile.Reader(fs, new Path(path), conf);
	    OneNodeKey key = (OneNodeKey) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
	    IntDegreeArray value = (IntDegreeArray) ReflectionUtils.newInstance(reader.getValueClass(), conf);

	    while (reader.next(key, value)) {
		edgeList.put(key.getNode(), new TIntArrayList(value.nodeArray));
	    }
	    IOUtils.closeStream(reader);
	}

	long Counter = 0;




	public void enumerate(int node0, int node1, int node2, int node3, int node4, Context context){

		++Counter;
//		context.getCounter("test","enumerate").increment(1);
	}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			super.cleanup(context);
			context.getCounter("test","enumerate").increment(Counter);
		}

		private void enumerateAll(String path, Context context, TIntObjectHashMap<TIntArrayList> edgeList)
		throws IllegalArgumentException, IOException {
	    Configuration conf = context.getConfiguration();

	    SequenceFile.Reader reader = null;
	    FileSystem fs = FileSystem.get(conf);
	    reader = new SequenceFile.Reader(fs, new Path(path), conf);
	    TwoNodeKey key = (TwoNodeKey) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
	    IntDegreeArray value = (IntDegreeArray) ReflectionUtils.newInstance(reader.getValueClass(), conf);

	    while (reader.next(key, value)) {
		enumerateOne(key, value, context, edgeList);
	    }
	    IOUtils.closeStream(reader);
	}

	private void enumerateOne(TwoNodeKey key, IntDegreeArray value, Context context,
		TIntObjectHashMap<TIntArrayList> edgeList) {
	    TIntHashSet theSet = new TIntHashSet();
	    TIntObjectHashMap<TIntArrayList> tp = new TIntObjectHashMap<TIntArrayList>();
		Boolean isEnumerating = context.getConfiguration().getBoolean("test.isEmitted", false);

	    int selected_node_id = 0;

	    // if (key.getNode1Degree() < key.getNode2Degree()) {
	    // selected_node_id = 1;
	    // }else if (key.getNode1Degree() == key.getNode2Degree()) {
	    // if (key.getNode1() < key.getNode2()) {
	    // selected_node_id = 1;
	    // }else {
	    // selected_node_id = 2;
	    // }
	    // }else {
	    // selected_node_id = 2;
	    // }
	    //
	    // int selected_node = (selected_node_id == 1 ? key.getNode1() :
	    // key.getNode2());
	    // int not_selected_node = (selected_node_id == 1 ? key.getNode2() :
	    // key.getNode1());
	    //

	    int selected_node = key.getNode1();
	    int not_selected_node = key.getNode2();
	    TIntArrayList theList = edgeList.get(selected_node);
	    if (map.get(not_selected_node) != null) {
		theSet.addAll(map.get(not_selected_node));

		for (int j = 0; j < theList.size(); ++j) {
		    int tpKey = theList.get(j);
		    if (map.containsKey(tpKey)) {
			TIntArrayList intersectList = map.get(tpKey);

			for (int i = 0; i < intersectList.size(); ++i) {
			    int node = intersectList.get(i);

			    if (tp.containsKey(node)) {
				tp.get(node).add(tpKey);
			    } else {
				if (theSet.contains(node)) {
				    TIntArrayList aList = new TIntArrayList();
				    aList.add(tpKey);
				    tp.put(node, aList);
				}

			    }
			}
		    }
		}

		TIntIterator iterator = tp.keySet().iterator();
		long counter = 0;

		while (iterator.hasNext()) {
		    int theKey = iterator.next();

		    if (theKey != selected_node) {
			TIntArrayList theValue = tp.get(theKey);
			for (int k = 0; k < value.size; k++) {

			    int valueK = value.getNode(k);
			    if (theKey != valueK) {
				for (int i = 0; i < theValue.size(); i++) {
				    if (theValue.get(i) != not_selected_node && theValue.get(i) != value.getNode(k)) {

				    	//counting
					++counter;


						if (isEnumerating) {
							//enumerating
						int node0 = key.node1;
						int node1 = valueK;
						int node2 = theKey;
						int node3 = theValue.get(i);
						int node4 = key.node2;

						enumerate(node0, node1, node2, node3, node4, context);
						}
					}


				}
			    }
			}
		    }
		}
		context.getCounter("test", "graph").increment(counter);
	    }

	}

	@Override
	protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
	    List<Integer> problems = getProblem(value);
	    TIntObjectHashMap<TIntArrayList> edgeList = new TIntObjectHashMap<TIntArrayList>();

	    Configuration conf = context.getConfiguration();
	    String path = conf.get("test.triangleIntermidiate1");

	    String inputPath = path + "-" + problems.get(0);
	    readEdge(inputPath, context, problems.get(0), edgeList);

	    String path2 = conf.get("test.triangleIntermidiate2");
	    String inputPath2 = path2 + "-" + problems.get(0);
	    enumerateAll(inputPath2, context, edgeList);
	}
    }

    private String createSeedFile(String seedFile, int p) throws IOException {
	Configuration conf = getConf();

	List<String> seedList = new ArrayList();

	FSDataOutputStream seedOut = FileSystem.get(conf).create(new Path(seedFile));

	for (int i = 0; i < p; i++) {
	    seedList.add(i + "\n");
	}

	Collections.shuffle(seedList);

	System.out.printf("[Info] %d subproblem(s) to process\n", Integer.valueOf(seedList.size()));

	for (String s : seedList) {
	    seedOut.writeBytes(s);
	}

	seedOut.close();

	return seedFile;
    }

    @Override
    public int run(String[] args) throws Exception {
	Configuration conf = getConf();

	FileSystem.get(conf).delete(new Path(args[0] + ".temp"));
	FileSystem.get(conf).delete(new Path(args[0] + ".temp2"));
	FileSystem.get(conf).delete(new Path(args[0] + ".seed"));

	// create seed file

	int p = conf.getInt("test.p", 1);
	String seed = args[0] + ".seed";
	createSeedFile(seed, 240);

	int memory_size = conf.getInt("test.memory", 4000);
	int opts_size = (int) (memory_size * 0.8);
	String memory_opts = "-Xmx" + opts_size + "M";

	conf.setInt("mapreduce.map.memory.mb", memory_size);
	conf.set("mapreduce.map.java.opts", memory_opts);
	conf.setInt("mapreduce.reduce.memory.mb", memory_size);
	conf.set("mapreduce.reduce.java.opts", memory_opts);
	conf.set("test.triangleIntermidiate1", args[0] + ".triangleIntermidiate/temp");
	conf.set("test.triangleIntermidiate2", args[0] + ".triangleIntermidiate2/temp");
	conf.setInt("test.p", p);

	FileSystem.get(conf).delete(new Path(conf.get("test.triangleIntermidiate1")));
	FileSystem.get(conf).delete(new Path(conf.get("test.triangleIntermidiate2")));
	FileSystem.get(conf).delete(new Path(args[0] + ".temp"));
	FileSystem.get(conf).delete(new Path(args[1] + ".temp2"));

	// job1
	Job job1 = new Job(conf);
	job1.setJarByClass(getClass());
	job1.setJobName("House1");

	job1.setInputFormatClass(SequenceFileInputFormat.class);

	job1.setMapperClass(HouseMPreprocessMapper1.class);
	job1.setReducerClass(HouseMPreprocessReducer1.class);

	FileInputFormat.setInputPaths(job1, args[0]);
	FileOutputFormat.setOutputPath(job1, new Path(args[0] + ".temp"));
	FileOutputFormat.setCompressOutput(job1, false);

	job1.setMapOutputKeyClass(SolarParitionKey.class);
	job1.setMapOutputValueClass(OneNodeKeyIntDegreeArray.class);

	job1.setOutputKeyClass(NullWritable.class);
	job1.setOutputValueClass(NullWritable.class);

	boolean success = job1.waitForCompletion(true);

	// job2

	Job job2 = new Job(conf);
	job2.setJarByClass(getClass());
	job2.setJobName("House2");

	job2.setInputFormatClass(SequenceFileInputFormat.class);
	job2.setOutputFormatClass(SequenceFileOutputFormat.class);
	job2.setMapperClass(HouseMPreprocessMapper2.class);
	job2.setReducerClass(HouseMPreprocessReducer2.class);

	FileInputFormat.setInputPaths(job2, args[1]);
	FileOutputFormat.setOutputPath(job2, new Path(args[1] + ".temp2"));

	job2.setMapOutputKeyClass(SolarParitionKey.class);
	job2.setMapOutputValueClass(TwoNodeKeyIntDegreeArray.class);

	job2.setOutputKeyClass(NullWritable.class);
	job2.setOutputValueClass(NullWritable.class);

	success = job2.waitForCompletion(true);
	// job3
	for (int i = 0; i < 240; ++i) {
	    // DistributedCache.addLocalFiles(getConf(), aString + iString);
	    org.apache.hadoop.mapreduce.filecache.DistributedCache.addCacheFile(new URI(args[0] + "/part-r-00" + appendNumberToLengthThree(i)), getConf());
	}

	Job job3 = new Job(conf);

	job3.setJarByClass(getClass());
	job3.setJobName("HouseS");

	job3.getConfiguration().setInt("mapred.line.input.format.linespermap", 1);

	FileInputFormat.setInputPaths(job3, new Path(seed));

	job3.setInputFormatClass(NLineInputFormat.class);
	job3.setOutputFormatClass(NullOutputFormat.class);

	job3.setNumReduceTasks(0);

	job3.setMapperClass(HouseMMapper.class);

	job3.waitForCompletion(true);

	return 0;
    }

    public static void main(String[] args) throws Exception {

	long startTime = 0;
	long endTime = 0;

	startTime = System.currentTimeMillis();
	JobConf conf = new JobConf();
	GenericOptionsParser paraser = new GenericOptionsParser(conf, args);
	// conf.setProfileEnabled(true);
	long milliSeconds = 10000 * 60 * 60; // <default is 600000, likewise can
					     // give any value)
	conf.setLong("mapred.task.timeout", milliSeconds);

	conf.setNumMapTasks(240);
	int p = conf.getInt("test.p", 1);

	for (int i = 0; i < p; i++) {
	    conf.setInt("test.offset", i);
	    ToolRunner.run(conf, new House(), args);
	}

	endTime = System.currentTimeMillis();
	log.info("[house] Time elapsed: " + (endTime - startTime) / 1000 + "s");
    }

}
