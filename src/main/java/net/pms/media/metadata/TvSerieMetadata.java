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

/**
 * This class keeps track of the TV serie metadata of media.
 */
public class TvSerieMetadata {
	/**
	 * Metadata gathered from either the filename or our API.
	 */
	private String createdBy;
	private String credits;
	private String endYear;
	private String externalIDs;
	private String firstAirDate;
	private String homepage;
	private String images;
	private String imdbID;
	private boolean inProduction;
	private String lastAirDate;
	private String languages;
	private String networks;
	private Double numberOfEpisodes;
	private Double numberOfSeasons;
	private String originalLanguage;
	private String originalTitle;
	private String originCountry;
	private String plot;
	private String productionCountries;
	private String productionCompanies;
	private String seasons;
	private String seriesType;
	private String simplifiedTitle;
	private String spokenLanguages;
	private String startYear;
	private String status;
	private String tagline;
	private String title;
	private Double totalSeasons;
	private String version;
	private String votes;

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String value) {
		this.createdBy = value;
	}

	public String getCredits() {
		return credits;
	}

	public void setCredits(String value) {
		this.credits = value;
	}

	public String getEndYear() {
		return endYear;
	}

	public void setEndYear(String value) {
		this.endYear = value;
	}

	public String getExternalIDs() {
		return externalIDs;
	}

	public void setExternalIDs(String value) {
		this.externalIDs = value;
	}

	public String getFirstAirDate() {
		return firstAirDate;
	}

	public void setFirstAirDate(String value) {
		this.firstAirDate = value;
	}

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String value) {
		this.homepage = value;
	}

	public String getImages() {
		return images;
	}

	public void setImages(String value) {
		this.images = value;
	}

	public boolean isInProduction() {
		return inProduction;
	}

	public void setInProduction(boolean value) {
		this.inProduction = value;
	}

	public String getIMDbID() {
		return imdbID;
	}

	public void setIMDbID(String value) {
		this.imdbID = value;
	}

	public String getLanguages() {
		return languages;
	}

	public void setLanguages(String value) {
		this.languages = value;
	}

	public String getLastAirDate() {
		return lastAirDate;
	}

	public void setLastAirDate(String value) {
		this.lastAirDate = value;
	}

	public String getNetworks() {
		return networks;
	}

	public void setNetworks(String value) {
		this.networks = value;
	}

	public Double getNumberOfEpisodes() {
		return numberOfEpisodes;
	}

	public void setNumberOfEpisodes(Double value) {
		this.numberOfEpisodes = value;
	}

	public Double getNumberOfSeasons() {
		return numberOfSeasons;
	}

	public void setNumberOfSeasons(Double value) {
		this.numberOfSeasons = value;
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

	public String getOriginCountry() {
		return originCountry;
	}

	public void setOriginCountry(String value) {
		this.originCountry = value;
	}

	public String getProductionCompanies() {
		return productionCompanies;
	}

	public void setProductionCompanies(String value) {
		this.productionCompanies = value;
	}

	public String getProductionCountries() {
		return productionCountries;
	}

	public void setProductionCountries(String value) {
		this.productionCountries = value;
	}

	public String getPlot() {
		return plot;
	}

	public void setPlot(String value) {
		this.plot = value;
	}

	public String getSeasons() {
		return seasons;
	}

	public void setSeasons(String value) {
		this.seasons = value;
	}

	public String getSeriesType() {
		return seriesType;
	}

	public void setSeriesType(String value) {
		this.seriesType = value;
	}

	public String getSimplifiedTitle() {
		return simplifiedTitle;
	}

	public void setSimplifiedTitle(String value) {
		this.simplifiedTitle = value;
	}

	public String getSpokenLanguages() {
		return spokenLanguages;
	}

	public void setSpokenLanguages(String value) {
		this.spokenLanguages = value;
	}

	public String getStartYear() {
		return startYear;
	}

	public void setStartYear(String value) {
		this.startYear = value;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String value) {
		this.status = value;
	}

	public String getTagline() {
		return tagline;
	}

	public void setTagline(String value) {
		this.tagline = value;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String value) {
		this.title = value;
	}

	public Double getTotalSeasons() {
		return totalSeasons;
	}

	public void setTotalSeasons(Double value) {
		this.totalSeasons = value;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String value) {
		this.version = value;
	}

	public String getVotes() {
		return votes;
	}

	public void setVotes(String value) {
		this.votes = value;
	}

}
