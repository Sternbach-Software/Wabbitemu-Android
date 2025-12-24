package io.github.angelsl.wabbitemu.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.AsyncTask;
import android.os.FileObserver;

public class FileUtils {

	private static class SingletonHolder {
		private static final FileUtils INSTANCE = new FileUtils();
	}

	public static FileUtils getInstance() {
		return SingletonHolder.INSTANCE;
	}

	private Set<String> mFiles = new HashSet<>();
	private CountDownLatch mSearchLatch;
	private final List<FileObserver> mObservers;

	protected FileUtils() {
		mObservers = new ArrayList<>();
		startFileSearch();
	}

	public void invalidateFiles() {
		startFileSearch();
	}

	private void startFileSearch() {
		final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				mSearchLatch = new CountDownLatch(1);
			}

			@Override
			public Void doInBackground(final Void... params) {
				final String path;
				if (StorageUtils.hasExternalStorage()) {
					path = StorageUtils.getPrimaryStoragePath();
					mFiles = findValidFiles(path);

					final String extraStorage = System.getenv("SECONDARY_STORAGE");
					final String extraStorageMore = System.getenv("EMULATED_STORAGE_TARGET");
					if (extraStorage != null && !"".equals(extraStorage) &&
							extraStorageMore != null && !"".equals(extraStorageMore))
					{
						for (final String dir : extraStorage.split(":")) {
							mFiles.addAll(findValidFiles(dir));
						}
					}
				} else {
					mFiles = new HashSet<>();
				}

				mSearchLatch.countDown();
				return null;
			}

			@Override
			protected void onPostExecute(final Void arg) {
			}
		};

		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public List<String> getValidFiles(final String extensionsRegex) {
		try {
			mSearchLatch.await();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return new ArrayList<>();
		}

		final List<String> validFiles = new ArrayList<>();
		final Pattern pattern = Pattern.compile(extensionsRegex, Pattern.CASE_INSENSITIVE);
		for (final String file : mFiles) {
			final Matcher matcher = pattern.matcher(file);
			if (matcher.find()) {
				validFiles.add(file);
			}
		}

		return validFiles;
	}

	private Set<String> findValidFiles(final String dir) {
		final Set<String> validFiles = new HashSet<>();

		final File rootDir = new File(dir);
		final File[] rootDirArray = rootDir.listFiles();
		if (rootDirArray == null) {
			return validFiles;
		}

		final LinkedList<File> filesToSearch = new LinkedList<>(Arrays.asList(rootDirArray));
		while (!filesToSearch.isEmpty()) {
			final File file = filesToSearch.removeFirst();

			if (file.isDirectory()) {
				final FileObserver observer = new SingleFileObserver(file.getPath(),
						FileObserver.CREATE | FileObserver.DELETE);
				mObservers.add(observer);
				observer.startWatching();

				final File[] subDirArray = file.listFiles(new WabbitFileFilter());
				if (subDirArray == null || subDirArray.length == 0) {
					continue;
				}

				Collections.addAll(filesToSearch, subDirArray);
			} else {
				final boolean isValid = isValidFile(file.getPath());
				if (isValid) {
					validFiles.add(file.getAbsolutePath());
				}
			}
		}

		return validFiles;
	}

	private boolean isValidFile(final String file) {
		int i = file.lastIndexOf('.');
		if (i <= 0) {
			return false;
		}

		if (file.length() != (i + 4)) {
			return false;
		}

		final char ext1 = Character.toLowerCase(file.charAt(++i));
		final char ext2 = Character.toLowerCase(file.charAt(++i));
		final char ext3 = Character.toLowerCase(file.charAt(++i));
		// Regex for reference
		// ".+\\.(rom|sav|([87][xc3265][bcdgiklmnpqstuvwyz]))$";
		if ((ext1 == 'r' && ext2 == 'o' && ext3 == 'm') ||
				(ext1 == 's' && ext2 == 'a' && ext3 == 'v'))
		{
			return true;
		}

		if (ext1 != '8' && ext1 != '7') {
			return false;
		}

		switch (ext2) {
		case 'x':
		case 'c':
		case '2':
		case '6':
		case '5':
			break;
		default:
			return false;
		}

		switch (ext3) {
		case 'b':
		case 'c':
		case 'd':
		case 'g':
		case 'i':
		case 'k':
		case 'l':
		case 'm':
		case 'n':
		case 'p':
		case 'q':
		case 's':
		case 't':
		case 'u':
		case 'v':
		case 'w':
		case 'x':
		case 'y':
		case 'z':
			break;
		default:
			return false;
		}

		return true;
	}

	private void handleFileEvent(final int event, final String path) {
		switch (event) {
		case FileObserver.CREATE:
			final File file = new File(path);
			if (file.isFile()) {
				mFiles.add(file.getAbsolutePath());
			}
			break;
		case FileObserver.DELETE:
			mFiles.remove(path);
			break;
		}
	}

	private class WabbitFileFilter implements FileFilter {

		private WabbitFileFilter() {
			// no-op
		}

		@Override
		public boolean accept(final File pathname) {
			return pathname.isDirectory() || isValidFile(pathname.getPath());

		}
	}

	private class SingleFileObserver extends FileObserver {
		private final String mPath;

		public SingleFileObserver(final String path, final int mask) {
			super(path, mask);
			mPath = path;
		}

		@Override
		public void onEvent(final int event, final String path) {
			final String newPath = mPath + "/" + path;
			handleFileEvent(event, newPath);
		}
	}
}
