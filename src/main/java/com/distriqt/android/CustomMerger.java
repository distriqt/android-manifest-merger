/**
 * @author Michael Archbold (https://github.com/marchbold)
 * @created 26/9/2022
 */
package com.distriqt.android;

/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.annotations.NonNull;
import com.android.manifmerger.ManifestMerger2;
import com.android.manifmerger.ManifestSystemProperty;
import com.android.manifmerger.MergingReport;
import com.android.utils.ILogger;
import com.android.utils.StdLogger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Command line interface to the {@link ManifestMerger2}
 */
public class CustomMerger
{

	public int process( String[] args ) throws FileNotFoundException
	{
		Iterator<String> arguments = Arrays.asList( args ).iterator();
		// first pass to get all mandatory parameters.
		String mainManifest = null;
		StdLogger.Level logLevel = StdLogger.Level.INFO;
		ILogger logger = new StdLogger( logLevel );
		while (arguments.hasNext())
		{
			String selector = arguments.next();
			if (!selector.startsWith( "--" ))
			{
				logger.error( null /* throwable */,
							  "Invalid parameter " + selector + ", expected a command switch" );
				return 1;
			}
			if ("--usage".equals( selector ))
			{
				usage();
				return 0;
			}
			// no value for --remove-tools-declarations and --disable-minSdkLibrary-check flag
			if ("--remove-tools-declarations".equals( selector ))
			{
				continue;
			}
			if ("--disable-minSdkLibrary-check".equals( selector ))
			{
				continue;
			}
			if (!arguments.hasNext())
			{
				logger.error( null /* throwable */,
							  "Command switch " + selector + " has no value associated" );
				return 1;
			}
			String value = arguments.next();

			if ("--main".equals( selector ))
			{
				mainManifest = value;
			}
			if ("--log".equals( selector ))
			{
				logLevel = StdLogger.Level.valueOf( value );
			}
		}

		if (mainManifest == null)
		{
			System.err.println( "--main command switch not provided." );
			return 1;
		}

		// recreate the logger with the provided log level for the rest of the processing.
		logger = createLogger( logLevel );
		File mainManifestFile = checkPath( mainManifest );
		ManifestMerger2.Invoker invoker = createInvoker(
				mainManifestFile, logger );

		// second pass, get optional parameters and store them in the invoker.
		arguments = Arrays.asList( args ).iterator();
		File outFile = null;

		while (arguments.hasNext())
		{
			String selector = arguments.next();
			if ("--remove-tools-declarations".equals( selector ))
			{
				invoker.withFeatures( ManifestMerger2.Invoker.Feature.REMOVE_TOOLS_DECLARATIONS );
				continue;
			}
			// Unsafely disables minSdkVersion check in libraries
			if ("--disable-minSdkLibrary-check".equals( selector ))
			{
				invoker.withFeatures( ManifestMerger2.Invoker.Feature.DISABLE_MINSDKLIBRARY_CHECK );
				continue;
			}
			String value = arguments.next();
			if (Strings.isNullOrEmpty( value ))
			{
				logger.error( null /* throwable */,
							  "Empty value for switch " + selector );
				return 1;
			}
			if ("--libs".equals( selector ))
			{
				StringTokenizer stringTokenizer = new StringTokenizer( value, File.pathSeparator );
				while (stringTokenizer.hasMoreElements())
				{
					File library = checkPath( stringTokenizer.nextToken() );
					invoker.addLibraryManifest( library );
				}
			}
			if ("--overlays".equals( selector ))
			{
				StringTokenizer stringTokenizer = new StringTokenizer( value, File.pathSeparator );
				while (stringTokenizer.hasMoreElements())
				{
					File library = checkPath( stringTokenizer.nextToken() );
					invoker.addFlavorAndBuildTypeManifest( library );
				}

			}
			if ("--property".equals( selector ))
			{
				if (!value.contains( "=" ))
				{
					logger.error( null /* throwable */,
								  "Invalid property setting, should be NAME=VALUE format" );
					return 1;
				}
				try
				{
					ManifestSystemProperty manifestElement =
							ManifestSystemProperty.valueOf( value.substring( 0, value.indexOf( '=' ) )
																 .toUpperCase( Locale.ENGLISH ) );
					invoker.setOverride( manifestElement, value.substring( value.indexOf( '=' ) + 1 ) );
				}
				catch (IllegalArgumentException e)
				{
					logger.error(
							e,
							"Invalid property name "
							+ value.substring( 0, value.indexOf( '=' ) )
							+ ", allowed properties are : "
							+ Joiner.on( ',' ).join( ManifestSystemProperty.getValues() ) );
					return 1;
				}
			}
			if ("--placeholder".equals( selector ))
			{
				if (!value.contains( "=" ))
				{
					logger.error( null /* throwable */,
								  "Invalid placeholder setting, should be NAME=VALUE format" );
					return 1;
				}
				invoker.setPlaceHolderValue( value.substring( 0, value.indexOf( '=' ) ),
											 value.substring( value.indexOf( '=' ) + 1 ) );
			}
			if ("--out".equals( selector ))
			{
				outFile = new File( value );
			}
		}
		try
		{
			MergingReport merge = invoker.merge();
			if (merge.getResult().isSuccess())
			{
				String mergedDocument =
						merge.getMergedDocument( MergingReport.MergedManifestKind.MERGED );
				if (mergedDocument != null)
				{
					if (outFile != null)
					{
						try
						{
							Files.asCharSink( outFile, Charsets.UTF_8 ).write( mergedDocument );
						}
						catch (IOException e)
						{
							throw new RuntimeException( e );
						}
					}
					else
					{
						System.out.println( mergedDocument );
					}
				}

//				System.out.println( "========================" );
//				for (MergingReport.Record record : merge.getLoggingRecords())
//				{
//					System.out.println( record );
//				}
//				for (Actions actions : merge.getActions().getAttributeRecords(  ) ))
//				{
//					System.out.println( record );
//				}
//				System.out.println( "========================" );
			}
			else
			{
				for (MergingReport.Record record : merge.getLoggingRecords())
				{
					System.err.println( record );
				}
				return 1;
			}
		}
		catch (ManifestMerger2.MergeFailureException e)
		{
			logger.error( e, "Exception while merging manifests" );
			return 1;
		}
		return 0;
	}

	protected ManifestMerger2.Invoker createInvoker( @NonNull File mainManifestFile,
													 @NonNull ILogger logger )
	{
		return ManifestMerger2.newMerger( mainManifestFile, logger, ManifestMerger2.MergeType.APPLICATION );
	}

	public static void usage()
	{
		System.out.println( "Android Manifest Merger Tool Version 2\n" );
		System.out.println( "Usage:" );
		System.out.println( "Merger --main mainAndroidManifest.xml" );
		System.out.println( "\t--log [VERBOSE, INFO, WARNING, ERROR]" );
		System.out.println( "\t--libs [path separated list of lib's manifests]" );
		System.out.println( "\t--overlays [path separated list of overlay's manifests]" );
		System.out.println(
				"\t--property ["
				+ Joiner.on( " | " ).join( ManifestSystemProperty.getValues() )
				+ "=value]" );
		System.out.println( "\t--placeholder [name=value]" );
		System.out.println( "\t--out [path of the output file]" );
		System.out.println( "\t--remove-tools-declarations" );
	}

	@VisibleForTesting
	protected File checkPath( @NonNull String path ) throws FileNotFoundException
	{
		@NonNull File file = new File( path );
		if (!file.exists())
		{
			System.err.println( path + " does not exist" );
			throw new FileNotFoundException( path );
		}
		return file;
	}

	@VisibleForTesting
	protected ILogger createLogger( @NonNull StdLogger.Level level )
	{
		return new StdLogger( level );
	}

}
