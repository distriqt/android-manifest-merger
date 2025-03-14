package com.distriqt.android;

public class ManifestMerger
{
	public static void main( String[] args )
	{
		try
		{
			System.exit( new CustomMerger().process( args ) );
		}
		catch (Exception e)
		{
			System.exit( 1 );
		}
		System.exit( 0 );
	}
}
