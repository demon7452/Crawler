package com.run;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {

	public static void main(String[] args) {
		ExecutorService pool = Executors.newCachedThreadPool();
		List<String> list = new ArrayList<String>();
		List<String> indexList = new ArrayList<String>();
		indexList.add("a");
		indexList.add("b");
		indexList.add("c");
		for(String inString : indexList){
			MyRunnable a = new MyRunnable(list, inString);
			pool.submit(a);
		}
		pool.shutdown();
		System.out.println(list);
	}

}

class MyRunnable implements Runnable{
	private String index;
	private List<String> list;
	public MyRunnable(List<String> list,String index){
		this.list = list;
		this.index = index;
	}

	public void run() {
		list.add(index);
	}
}
