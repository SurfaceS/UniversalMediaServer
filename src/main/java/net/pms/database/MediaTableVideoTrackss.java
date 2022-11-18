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
package net.pms.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import static net.pms.database.DatabaseHelper.SIZE_MAX;
import net.pms.media.Media;
import net.pms.media.MediaVideo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.media.MediaVideo.ScanOrder;
import net.pms.media.MediaVideo.ScanType;

/**
 * This class is responsible for managing the Videotracks releases table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class MediaTableVideoTrackss extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableVideoTrackss.class);
	public static final String TABLE_NAME = "VIDEOTRACKS";

	private static final String COL_FILEID = "FILEID";
	private static final String COL_WIDTH = "WIDTH";
	private static final String COL_HEIGHT = "HEIGHT";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	public static final String TABLE_COL_WIDTH = TABLE_NAME + "." + COL_WIDTH;
	public static final String TABLE_COL_HEIGHT = TABLE_NAME + "." + COL_HEIGHT;

	private static final String SQL_GET_ALL_FILEID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ?";

	private static final int SIZE_LANG = 3;
	private static final int SIZE_CODEC = 32;
	private static final int SIZE_CODEC_PROFILE = 16;
	private static final int SIZE_CODEC_LEVEL = 16;
	private static final int SIZE_FRAMERATE = 32;
	private static final int SIZE_FRAMERATE_MODE = 16;
	private static final int SIZE_MUXING_MODE = 32;
	private static final int SIZE_MATRIX_COEFFICIENTS = 16;

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		if (tableExists(connection, TABLE_NAME)) {
			Integer version = MediaTableTablesVersions.getTableVersion(connection, TABLE_NAME);
			if (version == null) {
				version = 1;
			}
			if (version < TABLE_VERSION) {
				upgradeTable(connection, version);
			} else if (version > TABLE_VERSION) {
				LOGGER.warn(LOG_TABLE_NEWER_VERSION, DATABASE_NAME, TABLE_NAME);
			}
		} else {
			createTable(connection);
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		}
	}

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				default -> throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE " + TABLE_NAME + " (");
			sb.append("  FILEID                    BIGINT           NOT NULL");
			sb.append(", ID                        INTEGER          NOT NULL");
			sb.append(", STREAM_ID                 INTEGER          NOT NULL");
			sb.append(", LANG                      VARCHAR(").append(SIZE_LANG).append(')');
			sb.append(", TITLE                     VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", CODEC                     VARCHAR(").append(SIZE_CODEC).append(')');
			sb.append(", CODEC_PROFILE             VARCHAR(").append(SIZE_CODEC_PROFILE).append(')');
			sb.append(", CODEC_LEVEL               VARCHAR(").append(SIZE_CODEC_LEVEL).append(')');
			sb.append(", WIDTH                     INTEGER");
			sb.append(", HEIGHT                    INTEGER");
			sb.append(", FRAMERATE                 VARCHAR(").append(SIZE_FRAMERATE).append(')');
			sb.append(", FRAMERATE_MODE            VARCHAR(").append(SIZE_FRAMERATE_MODE).append(')');
			sb.append(", ASPECT_RATIO_CONTAINER    VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", ASPECT_RATIO_VIDEOTRACK   VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", REF_FRAMES                TINYINT");
			sb.append(", MUXING_MODE               VARCHAR(").append(SIZE_MUXING_MODE).append(')');
			sb.append(", STEREOSCOPY               VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", MATRIX_COEFFICIENTS       VARCHAR(").append(SIZE_MATRIX_COEFFICIENTS).append(')');
			sb.append(", BIT_DEPTH                 INTEGER");
			sb.append(", SCAN_TYPE                 VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", SCAN_ORDER                VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", CONSTRAINT " + TABLE_NAME + "_PK PRIMARY KEY (FILEID, ID)");
			sb.append(", CONSTRAINT " + TABLE_NAME + "_" + COL_FILEID + "_FK FOREIGN KEY (" + COL_FILEID + ") REFERENCES " + MediaTableFiles.TABLE_NAME + "(" + MediaTableFiles.COL_ID + ") ON DELETE CASCADE");
			sb.append(')');

			executeUpdate(statement, sb.toString());
			execute(connection, "CREATE INDEX " + TABLE_NAME + "_" + COL_FILEID + "_" + COL_WIDTH + "_" + COL_HEIGHT + " ON " + TABLE_NAME + "(" + COL_FILEID + ", " + COL_WIDTH + ", " + COL_HEIGHT + ")");
		}
	}

	protected static void insertOrUpdateVideoTracks(Connection connection, long fileId, Media media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || media.getVideoTrackCount() < 1) {
			return;
		}

		String columns = "FILEID, ID, STREAM_ID, LANG, TITLE, CODEC, CODEC_PROFILE, CODEC_LEVEL, " +
					"WIDTH, HEIGHT, FRAMERATE, FRAMERATE_MODE, ASPECT_RATIO_CONTAINER, ASPECT_RATIO_VIDEOTRACK, " +
					"REF_FRAMES, MUXING_MODE, STEREOSCOPY, MATRIX_COEFFICIENTS, BIT_DEPTH, SCAN_TYPE, SCAN_ORDER";

		try (
			PreparedStatement updateStatment = connection.prepareStatement(
				"SELECT " + columns + " " +
				"FROM " + TABLE_NAME + " " +
				"WHERE FILEID = ? AND ID = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO " + TABLE_NAME + " (" + columns + ")" +
				createDefaultValueForInsertStatement(columns)
			);
		) {
			for (MediaVideo videoTrack : media.getVideoTracks()) {
				updateStatment.setLong(1, fileId);
				updateStatment.setInt(2, videoTrack.getId());
				try (ResultSet rs = updateStatment.executeQuery()) {
					if (rs.next()) {
						rs.updateInt("STREAM_ID", videoTrack.getStreamId());
						rs.updateString("LANG", StringUtils.left(videoTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", StringUtils.left(videoTrack.getTitle(), SIZE_MAX));
						rs.updateString("CODEC", StringUtils.left(videoTrack.getCodec(), SIZE_CODEC));
						if (videoTrack.getCodecProfile() == null) {
							rs.updateNull("CODEC_PROFILE");
						} else {
							rs.updateString("CODEC_PROFILE", StringUtils.left(videoTrack.getCodecProfile(), SIZE_CODEC_PROFILE));
						}
						if (videoTrack.getCodecLevel() == null) {
							rs.updateNull("CODEC_LEVEL");
						} else {
							rs.updateString("CODEC_LEVEL", StringUtils.left(videoTrack.getCodecLevel(), SIZE_CODEC_LEVEL));
						}
						rs.updateInt("WIDTH", videoTrack.getWidth());
						rs.updateInt("HEIGHT", videoTrack.getHeight());
						rs.updateString("FRAMERATE", StringUtils.left(videoTrack.getFrameRate(), SIZE_FRAMERATE));
						rs.updateString("FRAMERATE_MODE", StringUtils.left(videoTrack.getFrameRateMode(), SIZE_FRAMERATE_MODE));
						rs.updateString("ASPECT_RATIO_CONTAINER", StringUtils.left(videoTrack.getAspectRatioContainer(), SIZE_MAX));
						rs.updateString("ASPECT_RATIO_VIDEOTRACK", StringUtils.left(videoTrack.getAspectRatioVideoTrack(), SIZE_MAX));
						rs.updateByte("REF_FRAMES", videoTrack.getReferenceFrameCount());
						rs.updateString("MUXING_MODE", StringUtils.left(videoTrack.getMuxingMode(), SIZE_MUXING_MODE));
						rs.updateString("STEREOSCOPY", StringUtils.left(videoTrack.getStereoscopy(), SIZE_MAX));
						rs.updateString("MATRIX_COEFFICIENTS", StringUtils.left(videoTrack.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
						rs.updateInt("BIT_DEPTH", videoTrack.getBitDepth());
						ScanType scanType = videoTrack.getScanType();
						if (scanType == null) {
							rs.updateNull("SCAN_TYPE");
						} else {
							rs.updateString("SCAN_TYPE", scanType.toString());
						}
						ScanOrder scanOrder = videoTrack.getScanOrder();
						if (scanOrder == null) {
							rs.updateNull("SCAN_ORDER");
						} else {
							rs.updateString("SCAN_ORDER", scanOrder.toString());
						}
						rs.updateRow();
					} else {
						int databaseColumnIterator = 0;
						insertStatement.clearParameters();
						insertStatement.setLong(++databaseColumnIterator, fileId);
						insertStatement.setInt(++databaseColumnIterator, videoTrack.getId());
						insertStatement.setInt(++databaseColumnIterator, videoTrack.getStreamId());
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getLang(), SIZE_LANG));
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getTitle(), SIZE_MAX));
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getCodec(), SIZE_CODEC));
						if (videoTrack.getCodecProfile() == null) {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						} else {
							insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getCodecProfile(), SIZE_MAX));
						}
						if (videoTrack.getCodecLevel() == null) {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						} else {
							insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getCodecLevel(), SIZE_MAX));
						}
						insertStatement.setInt(++databaseColumnIterator, videoTrack.getWidth());
						insertStatement.setInt(++databaseColumnIterator, videoTrack.getWidth());
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getFrameRate(), SIZE_FRAMERATE));
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getFrameRateMode(), SIZE_FRAMERATE_MODE));
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getAspectRatioContainer(), SIZE_MAX));
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getAspectRatioVideoTrack(), SIZE_MAX));
						insertStatement.setByte(++databaseColumnIterator, videoTrack.getReferenceFrameCount());
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getMuxingMode(), SIZE_MUXING_MODE));
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getStereoscopy(), SIZE_MAX));
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoTrack.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
						insertStatement.setInt(++databaseColumnIterator, videoTrack.getBitDepth());
						ScanType scanType = videoTrack.getScanType();
						if (scanType == null) {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						} else {
							insertStatement.setString(++databaseColumnIterator, StringUtils.left(scanType.toString(), SIZE_MAX));
						}
						ScanOrder scanOrder = videoTrack.getScanOrder();
						if (scanOrder == null) {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						} else {
							insertStatement.setString(++databaseColumnIterator, StringUtils.left(scanOrder.toString(), SIZE_MAX));
						}
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}

	protected static List<MediaVideo> getVideoTracks(Connection connection, long fileId) {
		List<MediaVideo> result = new ArrayList<>();
		if (connection == null || fileId < 0) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_FILEID)) {
			stmt.setLong(1, fileId);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					MediaVideo video = new MediaVideo();
					video.setId(rs.getInt("ID"));
					video.setStreamId(rs.getInt("STREAM_ID"));
					video.setLang(rs.getString("LANG"));
					video.setTitle(rs.getString("TITLE"));
					video.setCodec(rs.getString("CODEC"));
					video.setCodecProfile(rs.getString("CODEC_PROFILE"));
					video.setCodecLevel(rs.getString("CODEC_LEVEL"));
					video.setWidth(rs.getInt("WIDTH"));
					video.setHeight(rs.getInt("HEIGHT"));
					video.setFrameRate(rs.getString("FRAMERATE"));
					video.setFrameRateMode(rs.getString("FRAMERATE_MODE"));
					video.setAspectRatioContainer(rs.getString("ASPECT_RATIO_CONTAINER"));
					video.setAspectRatioVideoTrack(rs.getString("ASPECT_RATIO_VIDEOTRACK"));
					video.setReferenceFrameCount(rs.getByte("REF_FRAMES"));
					video.setMuxingMode(rs.getString("MUXING_MODE"));
					video.setStereoscopy(rs.getString("STEREOSCOPY"));
					video.setMatrixCoefficients(rs.getString("MATRIX_COEFFICIENTS"));
					video.setBitDepth(rs.getInt("BIT_DEPTH"));
					video.setScanType(ScanType.typeOf(rs.getString("SCAN_TYPE")));
					video.setScanOrder(ScanOrder.typeOf(rs.getString("SCAN_ORDER")));
					LOGGER.trace("Adding video from the database: {}", video.toString());
					result.add(video);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}
}
