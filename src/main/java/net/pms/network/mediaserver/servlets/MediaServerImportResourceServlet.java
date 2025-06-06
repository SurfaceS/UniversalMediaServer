package net.pms.network.mediaserver.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import net.pms.network.HttpServletHelper;
import net.pms.store.MediaScanner;
import net.pms.store.MediaStoreIds;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "MEDIA SERVER IMPORT RESOURCE", urlPatterns = { "/import" }, displayName = "Media Server Import Resource Servlet")
public class MediaServerImportResourceServlet extends HttpServletHelper {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServerImportResourceServlet.class.getName());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//FIXME : this is disabled by default as it allow anomymous devices to write/delete/replace medias on the disks.
		//Common users do not allow this, it is a security breach.
		if (!CONFIGURATION.isAnonymousDevicesWrite()) {
			LOGGER.warn("Anonymous devices connot write on disk.");
			return;
		}

		resp.setContentType("text/plain");
		resp.setCharacterEncoding("utf-8");

		String id = req.getParameter("id");

		if (StringUtils.isBlank(id)) {
			LOGGER.warn("no StoreResource id submitted. Nothing to update.");
			return;
		}

		req.getParts().forEach((part) -> {
			File inFile = null;
			File outFile;
			try (InputStream inputStream = part.getInputStream()) {
				inFile = File.createTempFile("ums", "import");
				long filesize = part.getSize();
				LOGGER.info("Updating object with id {}. Expected filesize is {}", id, filesize);
				IO.copy(inputStream, new FileOutputStream(inFile));
				if (Files.size(inFile.toPath()) != filesize) {
					LOGGER.error("reported and transmitted file size do not match. StoreResource will not be updated.");
					return;
				}
				String filename = MediaStoreIds.getMediaStoreNameForId(id);
				if (filename != null) {
					outFile = new File(filename);
					IO.copy(inFile, outFile);
					LOGGER.info("Resource id {} imported/updated at {}", id, outFile.getAbsolutePath());
					MediaScanner.backgroundScanFileOrFolder(outFile.getParentFile().getAbsolutePath());
				} else {
					LOGGER.warn("Store resource with id {} not found. StoreResource will not be updated.", id);
				}
			} catch (Throwable x) {
				LOGGER.error("StoreResource upload error", x);
			} finally {
				if (inFile != null && inFile.exists()) {
					inFile.delete();
				}
			}
		});

		LOGGER.debug("finished updating resource.");
	}
}
