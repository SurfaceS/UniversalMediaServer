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
package net.pms.dlna;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the video properties of media.
 */
public class DLNAMediaVideo extends DLNAMediaLang implements Cloneable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaVideo.class);

	private String codec;
	private String muxingMode;
	private String frameRate;
	private String frameRateOriginal;
	private String frameRateMode;
	private String frameRateModeRaw;
	private String title;
	private int width;
	private int height;
	private int bitDepth;
	private String matrixCoefficients;
	private String pixelAspectRatio;
	private String stereoscopy;
	private String aspectRatioContainer;
	private String aspectRatioVideoTrack;
	private int videoStreamIndex;
	private boolean encrypted;

	private final ReentrantReadWriteLock referenceFrameCountLock = new ReentrantReadWriteLock();
	private byte referenceFrameCount = -1;
	private final ReentrantReadWriteLock codecLevelLock = new ReentrantReadWriteLock();
	private String codecLevel;
	private final Object codecProfileLock = new Object();
	private String codecProfile;
	private Map<String, String> extras;
	private ScanType scanType;
	private ScanOrder scanOrder;

	/**
	 * Returns the name of the video codec that is being used.
	 *
	 * @return The name of the video codec.
	 */
	public String getCodec() {
		return codec;
	}

	/**
	 * Sets the name of the video codec that is being used.
	 *
	 * @param codec The name of the video codec to set.
	 */
	public void setCodec(String codec) {
		this.codec = codec != null ? codec.toLowerCase(Locale.ROOT) : null;
	}

	/**
	 * @return Codec level for video stream or {@code null} if not parsed.
	 */
	public String getCodecLevel() {
		codecLevelLock.readLock().lock();
		try {
			return codecLevel;
		} finally {
			codecLevelLock.readLock().unlock();
		}
	}

	/**
	 * Sets Codec level for video stream or {@code null} if not parsed.
	 *
	 * @param codecLevel Codec level.
	 */
	public void setCodecLevel(String codecLevel) {
		codecLevelLock.writeLock().lock();
		try {
			this.codecLevel = codecLevel;
		} finally {
			codecLevelLock.writeLock().unlock();
		}
	}

	public String getCodecProfile() {
		synchronized (codecProfileLock) {
			return codecProfile;
		}
	}

	public void setCodecProfile(String value) {
		synchronized (codecProfileLock) {
			codecProfile = value;
		}
	}

	/**
	 * Returns the video stream index on the container.
	 *
	 * @return The video stream index.
	 */
	public int getVideoStreamIndex() {
		return videoStreamIndex;
	}

	/**
	 * Set the video stream index on the container.
	 */
	public void setVideoStreamIndex(int streamId) {
		this.videoStreamIndex = streamId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String value) {
		this.title = value;
	}

	/**
	 * @return the video width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @param width the video width to set
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @param height the height to set
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	public String getMatrixCoefficients() {
		return matrixCoefficients;
	}

	public void setMatrixCoefficients(String matrixCoefficients) {
		this.matrixCoefficients = matrixCoefficients;
	}

	/**
	 * @return The pixel aspect ratio.
	 */
	public String getPixelAspectRatio() {
		return pixelAspectRatio;
	}

	/**
	 * Sets the pixel aspect ratio.
	 *
	 * @param pixelAspectRatio the pixel aspect ratio to set.
	 */
	public void setPixelAspectRatio(String pixelAspectRatio) {
		this.pixelAspectRatio = pixelAspectRatio;
	}

	/**
	 * @return the video bit depth
	 */
	public int getBitDepth() {
		return bitDepth;
	}

	/**
	 * @param value the video bit depth to set
	 */
	public void setBitDepth(int value) {
		this.bitDepth = value;
	}

	/**
	 * Note: This is based on a flag in Matroska files, and as such it is
	 * unreliable; it will be unlikely to find a false-positive but there
	 * will be false-negatives, similar to language flags.
	 *
	 * @return the type of stereoscopy (3D) of the video track
	 */
	public String getStereoscopy() {
		return stereoscopy;
	}

	/**
	 * Sets the type of stereoscopy (3D) of the video track.
	 *
	 * Note: This is based on a flag in Matroska files, and as such it is
	 * unreliable; it will be unlikely to find a false-positive but there
	 * will be false-negatives, similar to language flags.
	 *
	 * @param stereoscopy the type of stereoscopy (3D) of the video track
	 */
	public void setStereoscopy(String stereoscopy) {
		this.stereoscopy = stereoscopy;
	}

	/**
	 * @return the {@link ScanType}.
	 */
	@Nullable
	public ScanType getScanType() {
		return scanType;
	}

	/**
	 * Sets the {@link ScanType}.
	 *
	 * @param scanType the {@link ScanType} to set.
	 */
	public void setScanType(@Nullable ScanType scanType) {
		this.scanType = scanType;
	}

	/**
	 * Sets the {@link ScanType} by parsing the specified {@link String}.
	 *
	 * @param scanType the {@link String} to parse.
	 */
	public void setScanType(@Nullable String scanType) {
		this.scanType = ScanType.typeOf(scanType);
	}

	/**
	 * @return the {@link ScanOrder}.
	 */
	@Nullable
	public ScanOrder getScanOrder() {
		return scanOrder;
	}

	/**
	 * Sets the {@link ScanOrder}.
	 *
	 * @param scanOrder the {@link ScanOrder} to set.
	 */
	public void setScanOrder(@Nullable ScanOrder scanOrder) {
		this.scanOrder = scanOrder;
	}

	/**
	 * Sets the {@link ScanOrder} by parsing the specified {@link String}.
	 *
	 * @param scanOrder the {@link String} to parse.
	 */
	public void setScanOrder(@Nullable String scanOrder) {
		this.scanOrder = ScanOrder.typeOf(scanOrder);
	}

	/**
	 * Get the aspect ratio reported by the file/container.
	 * This is the aspect ratio that the renderer should display the video
	 * at, and is usually the same as the video track aspect ratio.
	 *
	 * @return the aspect ratio reported by the file/container
	 */
	public String getAspectRatioContainer() {
		return aspectRatioContainer;
	}

	/**
	 * Sets the aspect ratio reported by the file/container.
	 *
	 * @param aspectRatio the aspect ratio to set.
	 */
	public void setAspectRatioContainer(String aspectRatio) {
		this.aspectRatioContainer = getFormattedAspectRatio(aspectRatio);
	}

	/**
	 * Get the aspect ratio of the video track. This is the actual aspect ratio
	 * of the pixels, which is not always the aspect ratio that the renderer
	 * should display or that we should output; that is
	 * {@link #getAspectRatioContainer()}
	 *
	 * @return the aspect ratio of the video track
	 */
	public String getAspectRatioVideoTrack() {
		return aspectRatioVideoTrack;
	}

	/**
	 * @param aspectRatio the aspect ratio to set
	 */
	public void setAspectRatioVideoTrack(String aspectRatio) {
		this.aspectRatioVideoTrack = getFormattedAspectRatio(aspectRatio);
	}

	/**
	 * @return the frame rate
	 */
	public String getFrameRate() {
		return frameRate;
	}

	/**
	 * @param frameRate the frame rate to set
	 */
	public void setFrameRate(String frameRate) {
		this.frameRate = frameRate;
	}

	/**
	 * @return the frameRateOriginal
	 */
	public String getFrameRateOriginal() {
		return frameRateOriginal;
	}

	/**
	 * @param frameRateOriginal the frameRateOriginal to set
	 */
	public void setFrameRateOriginal(String frameRateOriginal) {
		this.frameRateOriginal = frameRateOriginal;
	}

	/**
	 * @return the frameRateMode
	 */
	public String getFrameRateMode() {
		return frameRateMode;
	}

	/**
	 * @param frameRateMode the frameRateMode to set
	 */
	public void setFrameRateMode(String frameRateMode) {
		this.frameRateMode = frameRateMode;
	}

	/**
	 * @return The unaltered frame rate mode
	 */
	public String getFrameRateModeRaw() {
		return frameRateModeRaw;
	}

	/**
	 * @param frameRateModeRaw the unaltered frame rate mode to set
	 */
	public void setFrameRateModeRaw(String frameRateModeRaw) {
		this.frameRateModeRaw = frameRateModeRaw;
	}

	/**
	 * @return reference frame count for video stream or {@code -1} if not parsed.
	 */
	public byte getReferenceFrameCount() {
		referenceFrameCountLock.readLock().lock();
		try {
			return referenceFrameCount;
		} finally {
			referenceFrameCountLock.readLock().unlock();
		}
	}

	/**
	 * Sets reference frame count for video stream or {@code -1} if not parsed.
	 *
	 * @param referenceFrameCount reference frame count.
	 */
	public void setReferenceFrameCount(byte referenceFrameCount) {
		if (referenceFrameCount < -1) {
			throw new IllegalArgumentException("referenceFrameCount must be >= -1.");
		}
		referenceFrameCountLock.writeLock().lock();
		try {
			this.referenceFrameCount = referenceFrameCount;
		} finally {
			referenceFrameCountLock.writeLock().unlock();
		}
	}

	/**
	 * @return the muxingMode
	 */
	public String getMuxingMode() {
		return muxingMode;
	}

	/**
	 * @param muxingMode the muxingMode to set
	 */
	public void setMuxingMode(String muxingMode) {
		this.muxingMode = muxingMode;
	}

	/**
	 * @return the encrypted
	 */
	public boolean isEncrypted() {
		return encrypted;
	}

	/**
	 * @param encrypted the encrypted to set
	 */
	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	public Map<String, String> getExtras() {
		return extras;
	}

	public void putExtra(String key, String value) {
		if (extras == null) {
			extras = new HashMap<>();
		}

		extras.put(key, value);
	}

	/**
	 * Returns a string containing all identifying audio properties.
	 *
	 * @return The properties string.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (getLang() != null && !getLang().equals("und")) {
			result.append("Id: ").append(getId());
			result.append(", Language Code: ").append(getLang());
		}

		if (StringUtils.isNotBlank(getTitle())) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("Video Track Title: ").append(getTitle());
		}

		if (result.length() > 0) {
			result.append(", ");
		}
		result.append(" Codec: ").append(getCodec());
		if (StringUtils.isNotBlank(getCodecLevel())) {
			result.append(", Codec Level: ").append(getCodecLevel());
		}
		if (StringUtils.isNotBlank(getCodecProfile())) {
			result.append(", Codec Profile: ").append(getCodecProfile());
		}

		result.append(", Resolution: ").append(getWidth()).append(" x ").append(getHeight());
		if (aspectRatioContainer != null) {
			result.append(", Display Aspect Ratio: ").append(getAspectRatioContainer());
		}
		if (!"1.000".equals(getPixelAspectRatio())) {
			result.append(", Pixel Aspect Ratio: ").append(getPixelAspectRatio());
		}
		if (getBitDepth() != 8) {
			result.append(", Video Depth: ").append(getBitDepth());
		}
		if (scanType != null) {
			result.append(", Scan Type: ").append(getScanType());
		}
		if (scanOrder != null) {
			result.append(", Scan Order: ").append(getScanOrder());
		}
		result.append(", Bit Depth: ").append(getBitDepth());
		if (StringUtils.isNotBlank(getFrameRateMode())) {
			result.append(", Frame Rate Mode: ");
			result.append(getFrameRateMode());
			if (StringUtils.isNotBlank(getFrameRateModeRaw())) {
				result.append(" (").append(getFrameRateModeRaw()).append(")");
			}
		} else if (StringUtils.isNotBlank(getFrameRateModeRaw())) {
			result.append(", Frame Rate Mode Raw: ");
			result.append(getFrameRateModeRaw());
		}
		if (getReferenceFrameCount() > -1) {
			result.append(", Reference Frame Count: ").append(getReferenceFrameCount());
		}
		if (StringUtils.isNotBlank(getMuxingMode())) {
			result.append(", Muxing Mode: ").append(getMuxingMode());
		}
		if (StringUtils.isNotBlank(getMatrixCoefficients())) {
			result.append(", Matrix Coefficients: ").append(getMatrixCoefficients());
		}
		return result.toString();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * This takes an exact aspect ratio, and returns the closest common aspect
	 * ratio to that, so that e.g. 720x416 and 720x420 are the same.
	 *
	 * @param aspect
	 * @return an approximate aspect ratio
	 */
	private static String getFormattedAspectRatio(String aspect) {
		if (StringUtils.isBlank(aspect)) {
			return null;
		}

		if (aspect.contains(":")) {
			return aspect;
		}

		double exactAspectRatio = Double.parseDouble(aspect);
		if (exactAspectRatio >= 11.9 && exactAspectRatio <= 12.1) {
			return "12.00:1";
		} else if (exactAspectRatio >= 3.9 && exactAspectRatio <= 4.1) {
			return "4.00:1";
		} else if (exactAspectRatio >= 2.75 && exactAspectRatio <= 2.77) {
			return "2.76:1";
		} else if (exactAspectRatio >= 2.65 && exactAspectRatio <= 2.67) {
			return "24:9";
		} else if (exactAspectRatio >= 2.58 && exactAspectRatio <= 2.6) {
			return "2.59:1";
		} else if (exactAspectRatio >= 2.54  && exactAspectRatio <= 2.56) {
			return "2.55:1";
		} else if (exactAspectRatio >= 2.38 && exactAspectRatio <= 2.41) {
			return "2.39:1";
		} else if (exactAspectRatio > 2.36 && exactAspectRatio < 2.38) {
			return "2.37:1";
		} else if (exactAspectRatio >= 2.34 && exactAspectRatio <= 2.36) {
			return "2.35:1";
		} else if (exactAspectRatio >= 2.33 && exactAspectRatio < 2.34) {
			return "21:9";
		} else if (exactAspectRatio > 2.1  && exactAspectRatio < 2.3) {
			return "11:5";
		} else if (exactAspectRatio > 1.9 && exactAspectRatio < 2.1) {
			return "2.00:1";
		} else if (exactAspectRatio > 1.87  && exactAspectRatio <= 1.9) {
			return "1.896:1";
		} else if (exactAspectRatio >= 1.83 && exactAspectRatio <= 1.87) {
			return "1.85:1";
		} else if (exactAspectRatio >= 1.7 && exactAspectRatio <= 1.8) {
			return "16:9";
		} else if (exactAspectRatio >= 1.65 && exactAspectRatio <= 1.67) {
			return "15:9";
		} else if (exactAspectRatio >= 1.59 && exactAspectRatio <= 1.61) {
			return "16:10";
		} else if (exactAspectRatio >= 1.54 && exactAspectRatio <= 1.56) {
			return "14:9";
		} else if (exactAspectRatio >= 1.49 && exactAspectRatio <= 1.51) {
			return "3:2";
		} else if (exactAspectRatio > 1.42 && exactAspectRatio < 1.44) {
			return "1.43:1";
		} else if (exactAspectRatio > 1.372 && exactAspectRatio < 1.4) {
			return "11:8";
		} else if (exactAspectRatio > 1.35 && exactAspectRatio <= 1.372) {
			return "1.37:1";
		} else if (exactAspectRatio >= 1.3 && exactAspectRatio <= 1.35) {
			return "4:3";
		} else if (exactAspectRatio > 1.2 && exactAspectRatio < 1.3) {
			return "5:4";
		} else if (exactAspectRatio >= 1.18 && exactAspectRatio <= 1.195) {
			return "19:16";
		} else if (exactAspectRatio > 0.99 && exactAspectRatio < 1.1) {
			return "1:1";
		} else if (exactAspectRatio > 0.7 && exactAspectRatio < 0.9) {
			return "4:5";
		} else if (exactAspectRatio > 0.6 && exactAspectRatio < 0.7) {
			return "2:3";
		} else if (exactAspectRatio > 0.5 && exactAspectRatio < 0.6) {
			return "9:16";
		} else {
			return aspect;
		}
	}

	/**
	 * This {@code enum} represents the different video "scan types".
	 */
	public enum ScanType {

		/** Interlaced scan, any sub-type */
		INTERLACED,

		/** Mixed scan */
		MIXED,

		/** Progressive scan */
		PROGRESSIVE;

		@Override
		public String toString() {
			return switch (this) {
				case INTERLACED -> "Interlaced";
				case MIXED -> "Mixed";
				case PROGRESSIVE -> "Progressive";
				default -> name();
			};
		}

		public static ScanType typeOf(String scanType) {
			if (StringUtils.isBlank(scanType)) {
				return null;
			}
			scanType = scanType.trim().toLowerCase(Locale.ROOT);
			switch (scanType) {
				case "interlaced" -> {
					return INTERLACED;
				}
				case "mixed" -> {
					return MIXED;
				}
				case "progressive" -> {
					return PROGRESSIVE;
				}
				default -> {
					LOGGER.debug("Warning: Unrecognized ScanType \"{}\"", scanType);
					return null;
				}
			}
		}
	}

	/**
	 * This {@code enum} represents the video scan order.
	 */
	public enum ScanOrder {

		/** Bottom Field First */
		BFF,

		/** Bottom Field Only */
		BFO,

		/** Pulldown */
		PULLDOWN,

		/** 2:2:2:2:2:2:2:2:2:2:2:3 Pulldown */
		PULLDOWN_2_2_2_2_2_2_2_2_2_2_2_3,

		/** 2:3 Pulldown */
		PULLDOWN_2_3,

		/** Top Field First */
		TFF,

		/** Top Field Only */
		TFO;

		@Override
		public String toString() {
			return switch (this) {
				case BFF -> "Bottom Field First";
				case BFO -> "Bottom Field Only";
				case PULLDOWN -> "Pulldown";
				case PULLDOWN_2_2_2_2_2_2_2_2_2_2_2_3 -> "2:2:2:2:2:2:2:2:2:2:2:3 Pulldown";
				case PULLDOWN_2_3 -> "2:3 Pulldown";
				case TFF -> "Top Field First";
				case TFO -> "Top Field Only";
				default -> name();
			};
		}

		public static ScanOrder typeOf(String scanOrder) {
			if (StringUtils.isBlank(scanOrder)) {
				return null;
			}
			scanOrder = scanOrder.trim().toLowerCase(Locale.ROOT);
			switch (scanOrder) {
				case "bff", "bottom field first" -> {
					return BFF;
				}
				case "bfo", "bottom field only" -> {
					return BFO;
				}
				case "pulldown" -> {
					return PULLDOWN;
				}
				case "2:2:2:2:2:2:2:2:2:2:2:3 pulldown" -> {
					return PULLDOWN_2_2_2_2_2_2_2_2_2_2_2_3;
				}
				case "2:3 pulldown" -> {
					return PULLDOWN_2_3;
				}
				case "tff", "top field first" -> {
					return TFF;
				}
				case "tfo", "top field only" -> {
					return TFO;
				}
				default -> {
					LOGGER.debug("Warning: Unrecognized ScanOrder \"{}\"", scanOrder);
					if (scanOrder.contains("pulldown")) {
						return PULLDOWN;
					}
					return null;
				}
			}
		}
	}

}
