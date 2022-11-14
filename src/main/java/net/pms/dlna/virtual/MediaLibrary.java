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
package net.pms.dlna.virtual;

import net.pms.Messages;
import net.pms.database.MediaTableAudiotracks;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableFilesStatus;
import net.pms.database.MediaTableRegexpRules;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.database.MediaTableVideotracks;
import net.pms.util.FullyPlayedAction;

/**
 * This is the Media Library folder which contains dynamic folders populated
 * by the SQL (h2) database.
 */
public class MediaLibrary extends VirtualFolder {
	/**
	 * According to the Academy of Motion Picture Arts and Sciences, the American
	 * Film Institute, and the British Film Institute, a feature film runs for
	 * more than 40 minutes.
	 *
	 * @see https://www.oscars.org/sites/oscars/files/93aa_rules.pdf
	 * @see https://www.bfi.org.uk/bfi-national-archive/research-bfi-archive/bfi-filmography/bfi-filmography-faq
	 */
	private static final Double FORTY_MINUTES_IN_SECONDS = 2400.0;
	private static final String SELECT_FILES_STATUS_WHERE = "SELECT * " + MediaLibraryFolder.FROM_FILES_STATUS + "WHERE ";
	private static final String SELECT_FILES_STATUS_VIDEO_WHERE = "SELECT * " + MediaLibraryFolder.FROM_FILES_STATUS_VIDEOMETA + "WHERE ";
	private static final String IS_AUDIO = MediaTableFiles.TABLE_COL_FORMAT_TYPE + " = 1";
	private static final String IS_IMAGE = MediaTableFiles.TABLE_COL_FORMAT_TYPE + " = 2";
	private static final String IS_VIDEO = MediaTableFiles.TABLE_COL_FORMAT_TYPE + " = 4";
	private static final String IS_AUDIO_PLAYLIST = MediaTableFiles.TABLE_COL_FORMAT_TYPE + " = 16";
	private static final String IS_DVD_IMAGE = MediaTableFiles.TABLE_COL_FORMAT_TYPE + " = 32";
	private static final String AND_IS_NOT_FULLYPLAYED = " AND " + MediaTableFilesStatus.TABLE_COL_ISFULLYPLAYED + " IS NOT TRUE";
	private static final String AND_IS_TVEPISODE = " AND " + MediaTableVideoMetadata.TABLE_COL_ISTVEPISODE;
	private static final String AND_IS_NOT_TVEPISODE = " AND NOT " + MediaTableVideoMetadata.TABLE_COL_ISTVEPISODE;
	private static final String AND_IS_MOVIE = AND_IS_NOT_TVEPISODE + " AND " + MediaTableVideoMetadata.TABLE_COL_MEDIA_YEAR + " != '' AND DURATION > " + FORTY_MINUTES_IN_SECONDS;
	private static final String AND_IS_HD_VIDEO = " AND " + MediaTableFiles.TABLE_COL_ID + " IN (SELECT DISTINCT " + MediaTableVideotracks.TABLE_COL_FILEID + " FROM " + MediaTableVideotracks.TABLE_NAME + " WHERE " + MediaTableVideotracks.TABLE_COL_WIDTH + " > 864 AND " + MediaTableVideotracks.TABLE_COL_HEIGHT + " > 576)";
	private static final String AND_IS_SD_VIDEO = " AND " + MediaTableFiles.TABLE_COL_ID + " IN (SELECT DISTINCT " + MediaTableVideotracks.TABLE_COL_FILEID + " FROM " + MediaTableVideotracks.TABLE_NAME + " WHERE " + MediaTableVideotracks.TABLE_COL_WIDTH + " <= 864 AND " + MediaTableVideotracks.TABLE_COL_HEIGHT + " <= 576)";

	private boolean enabled;

	private MediaLibraryFolder allFolder;
	private MediaLibraryFolder albumFolder;
	private MediaLibraryFolder artistFolder;
	private MediaLibraryFolder genreFolder;
	private MediaLibraryFolder playlistFolder;
	private VirtualFolder vfAudio = null;

	public boolean isEnabled() {
		return enabled;
	}

	public MediaLibraryFolder getAllFolder() {
		return allFolder;
	}

	public VirtualFolder getAudioFolder() {
		return vfAudio;
	}

	public MediaLibraryFolder getAlbumFolder() {
		return albumFolder;
	}

	public MediaLibrary() {
		super(Messages.getString("MediaLibrary"), "/images/folder-icons/media-library.png");
		reset();
	}

	public final void reset() {
		enabled = configuration.getUseCache();
		if (!enabled) {
			return;
		}
		// Videos folder
		VirtualFolder vfVideo = new VirtualFolder(Messages.getString("Video"), null);

		// All videos that are unwatched
		MediaLibraryFolder unwatchedTvShowsFolder = new MediaLibraryFolder(
			Messages.getString("TvShows"),
			new String[]{
				"SELECT DISTINCT " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAME + MediaLibraryFolder.FROM_FILES_STATUS_VIDEOMETA + "WHERE " + IS_VIDEO + AND_IS_TVEPISODE + AND_IS_NOT_FULLYPLAYED + " ORDER BY " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAME + " ASC",
				SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + AND_IS_TVEPISODE + AND_IS_NOT_FULLYPLAYED + " AND " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAME + " = '${0}' ORDER BY " + MediaTableVideoMetadata.TABLE_COL_TVSEASON + ", " + MediaTableVideoMetadata.TABLE_COL_TVEPISODENUMBER
			},
			new int[]{MediaLibraryFolder.TVSERIES_WITH_FILTERS, MediaLibraryFolder.EPISODES}
		);

		MediaLibraryFolder unwatchedMoviesFolder = new MediaLibraryFolder(Messages.getString("Movies"), SELECT_FILES_STATUS_WHERE + IS_VIDEO + " " + AND_IS_MOVIE + " AND " + MediaTableFiles.TABLE_COL_STEREOSCOPY + " = ''" + AND_IS_NOT_FULLYPLAYED + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMovies3DFolder = new MediaLibraryFolder(Messages.getString("3dMovies"), SELECT_FILES_STATUS_WHERE + IS_VIDEO + " " + AND_IS_MOVIE + " AND " + MediaTableFiles.TABLE_COL_STEREOSCOPY + " != ''" + AND_IS_NOT_FULLYPLAYED + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedUnsortedFolder = new MediaLibraryFolder(Messages.getString("Unsorted"), SELECT_FILES_STATUS_WHERE + IS_VIDEO + " AND NOT " + MediaTableVideoMetadata.TABLE_COL_ISTVEPISODE + " AND (" + MediaTableVideoMetadata.TABLE_COL_MEDIA_YEAR + " IS NULL OR " + MediaTableVideoMetadata.TABLE_COL_MEDIA_YEAR + " = '')" + AND_IS_NOT_FULLYPLAYED + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedRecentlyAddedVideos = new MediaLibraryFolder(Messages.getString("RecentlyAdded"), SELECT_FILES_STATUS_WHERE + IS_VIDEO + "" + AND_IS_NOT_FULLYPLAYED + " ORDER BY " + MediaTableFiles.TABLE_COL_MODIFIED + " DESC LIMIT 100", MediaLibraryFolder.FILES_NOSORT);
		MediaLibraryFolder unwatchedAllVideosFolder = new MediaLibraryFolder(Messages.getString("AllVideos"), SELECT_FILES_STATUS_WHERE  + IS_VIDEO + "" + AND_IS_NOT_FULLYPLAYED + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo02 = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(" + MediaTableFiles.TABLE_COL_MODIFIED + ", 'yyyy MM d') " + MediaLibraryFolder.FROM_FILES_STATUS_VIDEOMETA + "WHERE " + IS_VIDEO + "" + AND_IS_NOT_FULLYPLAYED + " ORDER BY " + MediaTableFiles.TABLE_COL_MODIFIED + " DESC",
				SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + AND_IS_NOT_FULLYPLAYED + " AND FORMATDATETIME(" + MediaTableFiles.TABLE_COL_MODIFIED + ", 'yyyy MM d') = '${0}'" + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder unwatchedMlfVideo03 = new MediaLibraryFolder(Messages.getString("HdVideos"), SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + AND_IS_NOT_FULLYPLAYED + AND_IS_HD_VIDEO + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo04 = new MediaLibraryFolder(Messages.getString("SdVideos"), SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + AND_IS_NOT_FULLYPLAYED + AND_IS_SD_VIDEO + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC", MediaLibraryFolder.FILES);
		MediaLibraryFolder unwatchedMlfVideo05 = new MediaLibraryFolder(Messages.getString("DvdImages"), SELECT_FILES_STATUS_VIDEO_WHERE + IS_DVD_IMAGE + AND_IS_NOT_FULLYPLAYED + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC", MediaLibraryFolder.ISOS);

		// The following block contains all videos regardless of fully played status
		MediaLibraryFolder tvShowsFolder = new MediaLibraryFolder(
			Messages.getString("TvShows"),
			new String[]{
				"SELECT DISTINCT " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAME + " " + MediaLibraryFolder.FROM_FILES_VIDEOMETA + " WHERE " + IS_VIDEO + AND_IS_TVEPISODE + "                              ORDER BY " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAME + " ASC",
				"SELECT          *               " + MediaLibraryFolder.FROM_FILES_VIDEOMETA + " WHERE " + IS_VIDEO + AND_IS_TVEPISODE + " AND " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAME + " = '${0}' ORDER BY TVSEASON, TVEPISODENUMBER"
			},
			new int[]{MediaLibraryFolder.TVSERIES_WITH_FILTERS, MediaLibraryFolder.EPISODES}
		);
		MediaLibraryFolder moviesFolder = new MediaLibraryFolder(
			Messages.getString("Movies"),
			SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + " " + AND_IS_MOVIE + " AND " + MediaTableFiles.TABLE_COL_STEREOSCOPY + " = '' ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder movies3DFolder = new MediaLibraryFolder(
			Messages.getString("3dMovies"),
			SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + " " + AND_IS_MOVIE + " AND " + MediaTableFiles.TABLE_COL_STEREOSCOPY + " != '' ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder unsortedFolder = new MediaLibraryFolder(
			Messages.getString("Unsorted"),
			SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + " AND NOT " + MediaTableVideoMetadata.TABLE_COL_ISTVEPISODE + " AND (" + MediaTableVideoMetadata.TABLE_COL_MEDIA_YEAR + " IS NULL OR " + MediaTableVideoMetadata.TABLE_COL_MEDIA_YEAR + " = '') ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder allVideosFolder = new MediaLibraryFolder(
			Messages.getString("AllVideos"),
			SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder recentlyAddedVideos = new MediaLibraryFolder(
			Messages.getString("RecentlyAdded"),
			new String[]{"SELECT * " + MediaLibraryFolder.FROM_FILES_VIDEOMETA + " WHERE " + IS_VIDEO + " ORDER BY " + MediaTableFiles.TABLE_COL_MODIFIED + " DESC LIMIT 100"},
			new int[]{MediaLibraryFolder.FILES_NOSORT}
		);
		MediaLibraryFolder inProgressVideos = new MediaLibraryFolder(
			Messages.getString("InProgress"),
			SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + " AND " + MediaTableFilesStatus.TABLE_COL_DATELASTPLAY + " IS NOT NULL" + AND_IS_NOT_FULLYPLAYED + " ORDER BY " + MediaTableFilesStatus.TABLE_COL_DATELASTPLAY + " DESC LIMIT 100",
			MediaLibraryFolder.FILES_NOSORT
		);
		MediaLibraryFolder mostPlayedVideos = new MediaLibraryFolder(
			Messages.getString("MostPlayed"),
			SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + " AND " + MediaTableFilesStatus.TABLE_COL_DATELASTPLAY + " IS NOT NULL ORDER BY " + MediaTableFilesStatus.TABLE_COL_PLAYCOUNT + " DESC LIMIT 100",
			MediaLibraryFolder.FILES_NOSORT
		);
		MediaLibraryFolder mlfVideo02 = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(" + MediaTableFiles.TABLE_COL_MODIFIED + ", 'yyyy MM d') FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + IS_VIDEO + " ORDER BY " + MediaTableFiles.TABLE_COL_MODIFIED + " DESC",
				IS_VIDEO + " AND FORMATDATETIME(" + MediaTableFiles.TABLE_COL_MODIFIED + ", 'yyyy MM d') = '${0}' ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT_WITH_FILTERS, MediaLibraryFolder.FILES}
		);
		MediaLibraryFolder mlfVideo03 = new MediaLibraryFolder(
			Messages.getString("HdVideos"),
			SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + AND_IS_HD_VIDEO + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo04 = new MediaLibraryFolder(
			Messages.getString("SdVideos"),
			SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + AND_IS_SD_VIDEO + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo05 = new MediaLibraryFolder(
			Messages.getString("DvdImages"),
			SELECT_FILES_STATUS_VIDEO_WHERE + IS_DVD_IMAGE + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.ISOS_WITH_FILTERS
		);

		// If fully played videos are to be hidden
		if (configuration.getFullyPlayedAction() == FullyPlayedAction.HIDE_MEDIA) {
			vfVideo.addChild(unwatchedTvShowsFolder);
			vfVideo.addChild(unwatchedMoviesFolder);
			vfVideo.addChild(unwatchedMovies3DFolder);
			vfVideo.addChild(unwatchedUnsortedFolder);
			vfVideo.addChild(unwatchedRecentlyAddedVideos);
			vfVideo.addChild(inProgressVideos);
			vfVideo.addChild(unwatchedAllVideosFolder);
			vfVideo.addChild(unwatchedMlfVideo02);
			vfVideo.addChild(unwatchedMlfVideo03);
			vfVideo.addChild(unwatchedMlfVideo04);
			vfVideo.addChild(unwatchedMlfVideo05);
		// If fully played videos are NOT to be hidden
		} else {
			vfVideo.addChild(tvShowsFolder);
			vfVideo.addChild(moviesFolder);
			vfVideo.addChild(movies3DFolder);
			vfVideo.addChild(unsortedFolder);
			vfVideo.addChild(recentlyAddedVideos);
			if (configuration.isShowRecentlyPlayedFolder()) {
				MediaLibraryFolder recentlyPlayedVideos = new MediaLibraryFolder(
					Messages.getString("RecentlyPlayed"),
					SELECT_FILES_STATUS_VIDEO_WHERE + IS_VIDEO + " AND " + MediaTableFilesStatus.TABLE_COL_DATELASTPLAY + " IS NOT NULL ORDER BY " + MediaTableFilesStatus.TABLE_COL_DATELASTPLAY + " DESC LIMIT 100",
					MediaLibraryFolder.FILES_NOSORT
				);
				vfVideo.addChild(recentlyPlayedVideos);
			}
			vfVideo.addChild(inProgressVideos);
			vfVideo.addChild(mostPlayedVideos);
			vfVideo.addChild(allVideosFolder);
			vfVideo.addChild(mlfVideo02);
			vfVideo.addChild(mlfVideo03);
			vfVideo.addChild(mlfVideo04);
			vfVideo.addChild(mlfVideo05);
		}
		addChild(vfVideo);

		vfAudio = new VirtualFolder(Messages.getString("Audio"), null);
		allFolder = new MediaLibraryFolder(
			Messages.getString("AllAudioTracks"),
			"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.FILES
		);
		vfAudio.addChild(allFolder);
		playlistFolder = new MediaLibraryFolder(
			Messages.getString("AllAudioPlaylists"),
			"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + IS_AUDIO_PLAYLIST + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.PLAYLISTS
		);
		vfAudio.addChild(playlistFolder);
		artistFolder = new MediaLibraryFolder(
			Messages.getString("ByArtist"),
			new String[]{
				"SELECT DISTINCT COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") AS ARTIST FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " ORDER BY ARTIST ASC",
				"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + "  FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") = '${0}'"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(artistFolder);
		albumFolder = new MediaLibraryFolder(
			Messages.getString("ByAlbum"),
			new String[]{
				"SELECT DISTINCT " + MediaTableAudiotracks.TABLE_COL_ALBUM + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " ORDER BY " + MediaTableAudiotracks.TABLE_COL_ALBUM + " ASC",
				"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND " + MediaTableAudiotracks.TABLE_COL_ALBUM + " = '${0}'"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(albumFolder);
		genreFolder = new MediaLibraryFolder(
			Messages.getString("ByGenre"),
			new String[]{
				"SELECT DISTINCT " + MediaTableAudiotracks.TABLE_COL_GENRE + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " ORDER BY " + MediaTableAudiotracks.TABLE_COL_GENRE + " ASC",
				"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND " + MediaTableAudiotracks.TABLE_COL_GENRE + " = '${0}'"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(genreFolder);
		MediaLibraryFolder mlf6 = new MediaLibraryFolder(
			Messages.getString("ByArtistAlbum"),
			new String[]{
				"SELECT DISTINCT COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") AS ARTIST FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " ORDER BY ARTIST ASC",
				"SELECT DISTINCT " + MediaTableAudiotracks.TABLE_COL_ALBUM + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") = '${0}' ORDER BY " + MediaTableAudiotracks.TABLE_COL_ALBUM + " ASC",
				"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") = '${1}' AND " + MediaTableAudiotracks.TABLE_COL_ALBUM + " = '${0}' ORDER BY " + MediaTableAudiotracks.TABLE_COL_TRACK + " ASC, " + MediaTableFiles.TABLE_COL_FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlf6);
		MediaLibraryFolder mlf7 = new MediaLibraryFolder(
			Messages.getString("ByGenreArtistAlbum"),
			new String[]{
				"SELECT DISTINCT " + MediaTableAudiotracks.TABLE_COL_GENRE + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " ORDER BY " + MediaTableAudiotracks.TABLE_COL_GENRE + " ASC",
				"SELECT DISTINCT COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") AS ARTIST FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND " + MediaTableAudiotracks.TABLE_COL_GENRE + " = '${0}' ORDER BY ARTIST ASC",
				"SELECT DISTINCT " + MediaTableAudiotracks.TABLE_COL_ALBUM + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND " + MediaTableAudiotracks.TABLE_COL_GENRE + " = '${1}' AND COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") = '${0}' ORDER BY " + MediaTableAudiotracks.TABLE_COL_ALBUM + " ASC",
				"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND " + MediaTableAudiotracks.TABLE_COL_GENRE + " = '${2}' AND COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") = '${1}' AND " + MediaTableAudiotracks.TABLE_COL_ALBUM + " = '${0}' ORDER BY " + MediaTableAudiotracks.TABLE_COL_TRACK + " ASC, " + MediaTableFiles.TABLE_COL_FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlf7);
		MediaLibraryFolder mlfAudioDate = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(" + MediaTableFiles.TABLE_COL_MODIFIED + ", 'yyyy MM d') FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " ORDER BY " + MediaTableFiles.TABLE_COL_MODIFIED + " DESC",
				"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND FORMATDATETIME(" + MediaTableFiles.TABLE_COL_MODIFIED + ", 'yyyy MM d') = '${0}' ORDER BY " + MediaTableAudiotracks.TABLE_COL_TRACK + " ASC, " + MediaTableFiles.TABLE_COL_FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlfAudioDate);

		MediaLibraryFolder mlf8 = new MediaLibraryFolder(
			Messages.getString("ByLetterArtistAlbum"),
			new String[]{
				"SELECT " + MediaTableRegexpRules.TABLE_COL_ID + " FROM " + MediaTableRegexpRules.TABLE_NAME + " ORDER BY " + MediaTableRegexpRules.TABLE_COL_REGEXP_ORDER + " ASC",
				"SELECT DISTINCT COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") AS ARTIST FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND ARTIST REGEXP (SELECT " + MediaTableRegexpRules.TABLE_COL_REGEXP_RULE + " FROM " + MediaTableRegexpRules.TABLE_NAME + " WHERE " + MediaTableRegexpRules.TABLE_COL_ID + " = '${0}') ORDER BY ARTIST ASC",
				"SELECT DISTINCT " + MediaTableAudiotracks.TABLE_COL_ALBUM + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") = '${0}' ORDER BY " + MediaTableAudiotracks.TABLE_COL_ALBUM + " ASC",
				"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + ", " + MediaTableAudiotracks.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " AND " + IS_AUDIO + " AND COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") = '${1}' AND " + MediaTableAudiotracks.TABLE_COL_ALBUM + " = '${0}'"
			},
			new int[]{MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES}
		);
		vfAudio.addChild(mlf8);
		addChild(vfAudio);

		VirtualFolder vfImage = new VirtualFolder(Messages.getString("Photo"), null);
		MediaLibraryFolder mlfPhoto01 = new MediaLibraryFolder(
			Messages.getString("AllPhotos"),
			IS_IMAGE + " ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC",
			MediaLibraryFolder.FILES
		);
		vfImage.addChild(mlfPhoto01);
		MediaLibraryFolder mlfPhoto02 = new MediaLibraryFolder(
			Messages.getString("ByDate"),
			new String[]{
				"SELECT FORMATDATETIME(" + MediaTableFiles.TABLE_COL_MODIFIED + ", 'yyyy MM d') FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + IS_IMAGE + " ORDER BY " + MediaTableFiles.TABLE_COL_MODIFIED + " DESC",
				IS_IMAGE + " AND FORMATDATETIME(" + MediaTableFiles.TABLE_COL_MODIFIED + ", 'yyyy MM d') = '${0}' ORDER BY " + MediaTableFiles.TABLE_COL_FILENAME + " ASC"
			},
			new int[]{MediaLibraryFolder.TEXTS_NOSORT, MediaLibraryFolder.FILES}
		);
		vfImage.addChild(mlfPhoto02);
		addChild(vfImage);
	}

	public MediaLibraryFolder getArtistFolder() {
		return artistFolder;
	}

	public MediaLibraryFolder getGenreFolder() {
		return genreFolder;
	}

	public MediaLibraryFolder getPlaylistFolder() {
		return playlistFolder;
	}
}
