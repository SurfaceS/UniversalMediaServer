/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.platform.linux;

import com.sun.jna.Platform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.pms.util.FilePermissions;
import net.pms.util.FileUtil.InvalidFileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreedesktopTrash {
	private static final Logger LOGGER = LoggerFactory.getLogger(FreedesktopTrash.class);
	private static final Object HOME_FOLDER_LOCK = new Object();
	private static final String INFO = "info";
	private static final String FILES = "files";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static Path homeFolder = null;

	/**
	 * Not to be instantiated.
	 */
	private FreedesktopTrash() {
	}

	private static String generateRandomFileName(String fileName) {
		if (fileName.contains("/") || fileName.contains("\\")) {
			throw new IllegalArgumentException("Invalid file name");
		}

		String prefix;
		String suffix;
		if (fileName.contains(".")) {
			int i = fileName.lastIndexOf('.');
			prefix = fileName.substring(0, i);
			suffix = fileName.substring(i);
		} else {
			prefix = fileName;
			suffix = "";
		}

		long n = RANDOM.nextLong();
		n = (n == Long.MIN_VALUE) ? 0 : Math.abs(n);
		return prefix + Long.toString(n) + suffix;
	}

	private static Path getVerifiedPath(String location) {
		if (location != null && !location.trim().isEmpty()) {
			Path path = Paths.get(location);
			if (Files.exists(path)) {
				return path.toAbsolutePath();
			}
		}
		return null;
	}

	private static Path getHomeFolder() {
		synchronized (HOME_FOLDER_LOCK) {
			if (homeFolder == null) {
				homeFolder = getVerifiedPath(System.getenv("XDG_DATA_HOME"));
				if (homeFolder == null) {
					homeFolder = getVerifiedPath(System.getenv("HOME"));
				}
				if (homeFolder == null) {
					homeFolder = getVerifiedPath(System.getProperty("user.home"));
				}
			}
			// Make a copy so we don't have to hold the lock
			return Paths.get(homeFolder.toString());
		}
	}

	private static boolean verifyTrashFolder(Path path, boolean create) {
		FilePermissions permissions;
		try {
			permissions = new FilePermissions(path);
		} catch (FileNotFoundException e) {
			if (create) {
				LOGGER.trace("Trash folder \"{}\" doesn't exist, attempting to create it", path);
				try {
					Files.createDirectories(path);
				} catch (IOException e1) {
					LOGGER.debug("Could not create user trash folder \"{}\": {}", path, e1.getMessage());
					LOGGER.trace("", e1);
					return false;
				}
				try {
					permissions = new FilePermissions(path);
				} catch (FileNotFoundException e1) {
					LOGGER.error("Impossible situation in verifyTrashFolder()", e1);
					return false;
				}
			} else {
				LOGGER.trace("Trash folder \"{}\" doesn't exist", path);
				LOGGER.trace("", e);
				return false;
			}
		}
		if (!(permissions.isBrowsable() && permissions.isWritable() && permissions.isFolder())) {
			if (!permissions.isFolder()) {
				LOGGER.debug("Trash folder \"{}\" is not a folder", path);
			} else {
				LOGGER.debug("Insufficient permissions for trash folder \"{}\": {}", path, permissions.toString());
			}
			return false;
		}
		return true;
	}

	private static Path getTrashFolder(Path path) throws InvalidFileSystemException, IOException {
		UnixMountPoint pathMountPoint;
		try {
			pathMountPoint = UnixMountPoint.getMountPoint(path);
		} catch (InvalidFileSystemException e) {
			throw new InvalidFileSystemException("Invalid file system for file: " + path.toAbsolutePath(), e);
		}
		Path folder = getHomeFolder();
		Path trashFolder;
		if (folder != null) {
			UnixMountPoint homeMountPoint = null;
			try {
				homeMountPoint = UnixMountPoint.getMountPoint(folder);
			} catch (InvalidFileSystemException e) {
				LOGGER.trace(e.getMessage(), e);
				// homeMountPoint == null is ok, fails on .equals()
			}
			if (pathMountPoint.equals(homeMountPoint)) {
				// The file is on the same partition as the home folder,
				// use home folder Trash
				trashFolder = Paths.get(folder.toString(), ".Trash");
				if (!Files.exists(trashFolder)) {
					// This is outside specification but follows convention
					trashFolder = Paths.get(folder.toString(), ".local/share/Trash");
				}
				if (verifyTrashFolder(trashFolder, true)) {
					return trashFolder;
				} else {
					return null;
				}
			}
		}

		// The file is on a different partition than the home folder
		// or no home folder was found, look for $topdir/.Trash.
		trashFolder = Paths.get(pathMountPoint.getFolder(), ".Trash");
		if (Files.exists(trashFolder, LinkOption.NOFOLLOW_LINKS)) {
			if (!Files.isSymbolicLink(trashFolder)) {
				try {
					if (isUnixStickyBit(trashFolder)) {
						if (verifyTrashFolder(trashFolder, false)) {
							try {
								trashFolder = Paths.get(trashFolder.toString(), String.valueOf(getUnixUID()));
								if (verifyTrashFolder(trashFolder, true)) {
									return trashFolder;
								} else {
									LOGGER.trace("Could not read or create trash folder \"{}\", trying next option", trashFolder);
								}
							} catch (IOException e) {
								LOGGER.trace("Could not determine user id while resolving trash folder, trying next option", e);
							}
						} else {
							LOGGER.trace("Insufficient permissions for trash folder \"{}\", trying next option", trashFolder);
						}
					} else {
						LOGGER.trace("Trash folder \"{}\" doesn't have sticky bit set, trying next option", trashFolder);
					}
				} catch (IOException e) {
					LOGGER.trace("Could not determine sticky bit for trash folder \"" + trashFolder + "\", trying next option", e);
				}
			} else {
				LOGGER.trace("Trash folder \"{}\" is a symbolic link, trying next option");
			}
		} else {
			LOGGER.trace("Trash folder \"{}\" doesn't exist, trying next option", trashFolder);
		}

		// $topdir/.Trash not found, looking for $topdir/.Trash-$uid
		try {
			trashFolder = Paths.get(pathMountPoint.getFolder(), ".Trash-" + getUnixUID());
		} catch (IOException e) {
			throw new IOException("Could not determine user id while resolving trash folder: " + e.getMessage(), e);
		}
		if (verifyTrashFolder(trashFolder, true)) {
			return trashFolder;
		} else {
			LOGGER.debug("Unable to read or create trash folder \"{}\"", trashFolder);
			return null;
		}
	}

	private static boolean verifyTrashStructure(Path trashPath) {
		String trashLocation = trashPath.toAbsolutePath().toString();
		return verifyTrashFolder(Paths.get(trashLocation, INFO), true) && verifyTrashFolder(Paths.get(trashLocation, FILES), true);
	}

	private static void moveToTrash(Path path) throws InvalidFileSystemException, IOException {
		if (path == null) {
			throw new NullPointerException("path cannot be null");
		}

		final int limit = 10;
		path = path.toAbsolutePath();
		FilePermissions pathPermissions = new FilePermissions(path);
		if (!pathPermissions.isReadable() || !pathPermissions.isWritable()) {
			throw new IOException("Insufficient permission to delete \"" + path.toString() + "\" - move to trash bin failed");
		}

		Path trashFolder = getTrashFolder(path);
		Path infoFolder = trashFolder.resolve(INFO);
		Path filesFolder = trashFolder.resolve(FILES);
		if (!verifyTrashFolder(infoFolder, true) || !verifyTrashFolder(filesFolder, true)) {
			throw new IOException(
				"Could not move \"" + path.toString() + "\" to trash bin because " +
				"of insufficient permissions for trash bin \"" + trashFolder.toString() + "\""
			);
		}

		// Create the trash info
		List<String> infoContent = new ArrayList<>();
		infoContent.add("[Trash Info]");
		infoContent.add("Path=" + URLEncoder.encode(path.toString(), Charset.defaultCharset().name()));
		infoContent.add("DeletionDate=" + new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").format(new Date()));

		// Create the trash info file
		Path infoFile = path.getFileName();
		String fileName = infoFile != null ? infoFile.toString() : "";
		int count = 0;
		boolean created = false;
		while (!created && count < limit) {
			infoFile = infoFolder.resolve(fileName + ".trashinfo");
			try {
				Files.write(infoFile, infoContent, Charset.defaultCharset(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
				created = true;
			} catch (IOException e) {
				fileName = generateRandomFileName(fileName);
				count++;
			}
		}
		if (!created) {
			throw new IOException("Could not find a target filename for \"" + path.toString() + "\" in trash bin");
		}

		Path targetPath = filesFolder.resolve(fileName);
		if (Files.exists(targetPath)) {
			throw new IOException(
				"Could not move \"" + path.toString() + "\" to trash bin since the trash bin \"" +
				trashFolder.toString() + "\" is corrupted"
			);
		}

		// Move the actual files
		Files.move(path, targetPath, StandardCopyOption.ATOMIC_MOVE);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{}{}\" moved to \"{}\"", Files.isDirectory(path) ? "Folder \"" : "File \"", path, targetPath);
		}
	}

	public static void moveToTrash(File file) throws InvalidFileSystemException, IOException {
		moveToTrash(file.toPath());
	}

	private static final int S_ISVTX = 512; // Unix sticky bit mask
	public static boolean isUnixStickyBit(Path path) throws IOException, InvalidFileSystemException {
		PosixFileAttributes attr = Files.readAttributes(path, PosixFileAttributes.class);
		try {
			Field stModeField = attr.getClass().getDeclaredField("st_mode");
			stModeField.setAccessible(true);
			int stMode = stModeField.getInt(attr);
			return (stMode & S_ISVTX) > 0;
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new InvalidFileSystemException("File is not on a Unix file system: " + e.getMessage(), e);
		}
	}

	private static int unixUID = Integer.MIN_VALUE;
	private static final Object UNIX_UID_LOCK = new Object();

	/**
	 * Gets the user ID on Unix based systems. This should not change during a
	 * session and the lookup is expensive, so we cache the result.
	 *
	 * @return The Unix user ID
	 * @throws IOException
	 */
	public static int getUnixUID() throws IOException {
		if (
			Platform.isAIX() || Platform.isFreeBSD() || Platform.isGNU() || Platform.iskFreeBSD() ||
			Platform.isLinux() || Platform.isMac() || Platform.isNetBSD() || Platform.isOpenBSD() ||
			Platform.isSolaris()
		) {
			synchronized (UNIX_UID_LOCK) {
				if (unixUID < 0) {
					String response;
					Process id;
					id = Runtime.getRuntime().exec(new String[] {"id", "-u"});
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(id.getInputStream(), Charset.defaultCharset()))) {
						response = reader.readLine();
					}

					try {
						unixUID = Integer.parseInt(response);
					} catch (NumberFormatException e) {
						throw new UnsupportedOperationException("Unexpected response from OS: " + response, e);
					}
				}

				return unixUID;
			}
		}
		throw new UnsupportedOperationException("getUnixUID can only be called on Unix based OS'es");
	}

}
