package com.distriqt.android;

import com.android.manifmerger.Merger;

import java.io.FileNotFoundException;

public class ManifestMerger
{
	public static void main(String[] args)
	{
		try
		{
			System.exit(new Merger().process(args));
		}
		catch (FileNotFoundException e)
		{
			System.exit(1);
		}
		System.exit(0);
	}
}
