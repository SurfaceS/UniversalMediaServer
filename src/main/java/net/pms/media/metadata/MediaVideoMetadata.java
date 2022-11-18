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
package net.pms.media.metadata;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the metadata of media.
 */
public class MediaVideoMetadata {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaVideoMetadata.class);
	private static final Gson GSON = new Gson();

	/**
	 * Metadata gathered from either the filename or our API.
	 */
	private String imdbID;
	private String year;
	private String tvShowName;
	private String simplifiedTvShowName;
	private String tvSeason;
	private String tvEpisodeNumber;
	private String tvEpisodeName;
	private String tvSeriesStartYear;
	private String extraInformation;
	private boolean isTVEpisode;
	/**
	 * Metadata gathered from our API.
	 */
	private List<String> actors;
	private String award;
	private Long budget;
	private List<String> countries;
	private String credits;
	private List<String> directors;
	private ApiExternalIDs externalIDs;
	private List<String> genres;
	private String homepage;
	private ApiImages images;
	private String imdbRating;
	private String originalLanguage;
	private String originalTitle;
	private String poster;
	private String production;
	private String productionCompanies;
	private String productionCountries;
	private String rated;
	private List<ApiRatingSource> ratings;
	private String released;
	private Long revenue;
	private TvSerieMetadata serieMetadata;

	public String getIMDbID() {
		return imdbID;
	}

	public void setIMDbID(String value) {
		this.imdbID = value;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String value) {
		this.year = value;
	}

	public String getTVSeriesStartYear() {
		return tvSeriesStartYear;
	}

	public void setTVSeriesStartYear(String value) {
		this.tvSeriesStartYear = value;
	}

	public String getMovieOrShowName() {
		return tvShowName;
	}

	public void setMovieOrShowName(String value) {
		this.tvShowName = value;
	}

	public String getSimplifiedMovieOrShowName() {
		return simplifiedTvShowName;
	}

	public void setSimplifiedMovieOrShowName(String value) {
		this.simplifiedTvShowName = value;
	}

	public String getTVSeason() {
		return tvSeason;
	}

	public void setTVSeason(String value) {
		this.tvSeason = value;
	}

	public String getTVEpisodeNumber() {
		return tvEpisodeNumber;
	}

	public String getTVEpisodeNumberUnpadded() {
		if (StringUtils.isNotBlank(tvEpisodeNumber) && tvEpisodeNumber.length() > 1 && tvEpisodeNumber.startsWith("0")) {
			return tvEpisodeNumber.substring(1);
		}
		return tvEpisodeNumber;
	}

	public void setTVEpisodeNumber(String value) {
		this.tvEpisodeNumber = value;
	}

	public String getTVEpisodeName() {
		return tvEpisodeName;
	}

	public void setTVEpisodeName(String value) {
		this.tvEpisodeName = value;
	}

	public boolean isTVEpisode() {
		return isTVEpisode;
	}

	public void setIsTVEpisode(boolean value) {
		this.isTVEpisode = value;
	}

	/**
	 * Any extra information like movie edition or whether it is a
	 * sample video.
	 *
	 * Example: "(Director's Cut) (Sample)"
	 * @return
	 */
	public String getExtraInformation() {
		return extraInformation;
	}

	/*
	 * Any extra information like movie edition or whether it is a
	 * sample video.
	 *
	 * Example: "(Director's Cut) (Sample)"
	 */
	public void setExtraInformation(String value) {
		this.extraInformation = value;
	}

	public List<String> getActors() {
		return actors;
	}

	public void setActors(List<String> value) {
		this.actors = value;
	}

	public String getAward() {
		return award;
	}

	public void setAward(String value) {
		this.award = value;
	}

	public Long getBudget() {
		return budget;
	}

	public void setBudget(Long value) {
		this.budget = value;
	}

	public List<String> getCountries() {
		return countries;
	}

	public void setCountries(List<String> value) {
		this.countries = value;
	}

	public String getCredits() {
		return credits;
	}

	public void setCredits(String value) {
		this.credits = value;
	}

	public List<String> getDirectors() {
		return directors;
	}

	public void setDirectors(List<String> value) {
		this.directors = value;
	}

	public ApiExternalIDs getExternalIDs() {
		return externalIDs;
	}

	public void setExternalIDs(ApiExternalIDs value) {
		this.externalIDs = value;
	}

	public void setExternalIDs(String value) {
		try {
			this.externalIDs = GSON.fromJson(value, ApiExternalIDs.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing ExternalIDs: {}", e.getMessage());
			this.externalIDs = null;
		}
	}

	public List<String> getGenres() {
		return genres;
	}

	public void setGenres(List<String> value) {
		this.genres = value;
	}

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String value) {
		this.homepage = value;
	}

	public ApiImages getImages() {
		return images;
	}

	public void setImages(ApiImages value) {
		this.images = value;
	}

	public void setImages(String value) {
		try {
			this.images = GSON.fromJson(value, ApiImages.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing Images: {}", e.getMessage());
			this.images = null;
		}
	}

	public String getImdbRating() {
		return imdbRating;
	}

	public void setImdbRating(String value) {
		this.imdbRating = value;
	}

	public String getOriginalLanguage() {
		return originalLanguage;
	}

	public void setOriginalLanguage(String value) {
		this.originalLanguage = value;
	}

	public String getOriginalTitle() {
		return originalTitle;
	}

	public void setOriginalTitle(String value) {
		this.originalTitle = value;
	}

	public String getPoster() {
		return poster;
	}

	public void setPoster(String value) {
		this.poster = value;
	}

	public String getProduction() {
		return production;
	}

	public void setProduction(String value) {
		this.production = value;
	}

	public String setProductionCompanies() {
		return productionCompanies;
	}

	public void setProductionCompanies(String value) {
		this.productionCompanies = value;
	}

	public String setProductionCountries() {
		return productionCountries;
	}

	public void setProductionCountries(String value) {
		this.productionCountries = value;
	}

	public String getRated() {
		return rated;
	}

	public void setRated(String value) {
		this.rated = value;
	}

	public List<ApiRatingSource> getRatings() {
		return ratings;
	}

	public void setRatings(List<ApiRatingSource> value) {
		this.ratings = value;
	}

	public String getReleased() {
		return released;
	}

	public void setReleased(String value) {
		this.released = value;
	}

	public Long getRevenue() {
		return revenue;
	}

	public void setRevenue(Long value) {
		this.revenue = value;
	}

	public TvSerieMetadata getSerieMetadata() {
		return serieMetadata;
	}

	public void setSerieMetadata(TvSerieMetadata value) {
		this.serieMetadata = value;
	}
}
