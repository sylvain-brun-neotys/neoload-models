package com.neotys.neoload.model.writers.neoload;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.neotys.neoload.model.ImmutableProject;
import com.neotys.neoload.model.Project;
import com.neotys.neoload.model.repository.ImmutableFileVariable;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.neotys.neoload.model.repository.FileVariable;
import com.neotys.neoload.model.repository.Variable;

public class NeoLoadWriter {

	static Logger logger = LoggerFactory.getLogger(NeoLoadWriter.class);
	
	private static final String PROJECT_VERSION = "6.4";
	private static final String PRODUCT_VERSION = "6.6.0";

	public enum ConfigFiles {
								REPOSITORY("repository.xml"),
								SCENARIO("scenario.xml"),
								SETTINGS("settings.xml");

		private String fileName;

		ConfigFiles(String fileName) {
			this.fileName = fileName;
		}
	}

	private Project project;
	private final String nlProjectFolder;
	Map<String, List<File>> fileToCopy;

	public NeoLoadWriter(final Project project, final String nlProjectFolder, final Map<String, List<File>> map) {
		this.project = project;
		this.nlProjectFolder = nlProjectFolder;
		this.fileToCopy = map;
	}

	public String getOutputFolder() {
		return nlProjectFolder;
	}

	public void write() {
		try {
			final File f = new File(nlProjectFolder);
			if (!f.exists()) {
				logger.info("Output folder does not exist, creating it.");
				Files.createDirectories(Paths.get(nlProjectFolder));
			} else if (f.isFile()) {
				logger.error("The destination is not a directory, migration aborted.");
				return;
			}
			writeXML();
			writeNLP(project.getName());
			logger.info("Project saved.");

		} catch (ParserConfigurationException | TransformerException | IOException e) {
			logger.error("Error writing project.", e);
		}
	}

	private void writeXML() throws ParserConfigurationException, TransformerException, IOException {

		StreamResult result = new StreamResult(new File(nlProjectFolder, ConfigFiles.REPOSITORY.fileName));

		//copy the file needed to NeoLoad Directory
		copyDataFilesToDestFolder(nlProjectFolder);
		changeBaseNameForCopiedVariables();

		// write the repository
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document doc = docBuilder.newDocument();
		ProjectWriter.of(project).writeXML(doc, nlProjectFolder);

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);

		transformer.transform(source, result);

		// extract scenario / settings
		FileUtils.copyInputStreamToFile(NeoLoadWriter.class.getResourceAsStream(ConfigFiles.SCENARIO.fileName),
				new File(nlProjectFolder, "scenario.xml"));
		FileUtils.copyInputStreamToFile(NeoLoadWriter.class.getResourceAsStream(ConfigFiles.SETTINGS.fileName),
				new File(nlProjectFolder, "settings.xml"));
		createConfigZip();
		deleteConfigFiles();
	}

	private void changeBaseNameForCopiedVariables() {
		ImmutableProject tmpProject = (ImmutableProject) project;
		List<Variable> newVariableList = new ArrayList<>();
		List<String> fileLst = new ArrayList<>();
		if (fileToCopy == null || fileToCopy.size() == 0) {
			return;
		}
		fileToCopy.get("variables").forEach(var -> fileLst.add(var.getAbsolutePath()));
		tmpProject.getVariables().stream().forEach(var -> {
			if (var instanceof FileVariable && ((FileVariable) var).getFileName().isPresent()
					&& fileLst.contains(((FileVariable) var).getFileName().get())) {
				ImmutableFileVariable fileVar = (ImmutableFileVariable) var;
				newVariableList.add(fileVar.withFileName(
						FileVariableWriter.VARIABLE_DIRECTORY + File.separator + new File(fileVar.getFileName().get()).getName()));
			} else {
				newVariableList.add(var);
			}
		});
		project = tmpProject.withVariables(newVariableList);
	}

	/**
	 * Copy parameters data files from LR project to NL project
	 * @param initFolderFullPath
	 * @param destFolderFullPath
	 */
	private void copyDataFilesToDestFolder(String destFolderFullPath) {
		if (fileToCopy == null)
			return;

		fileToCopy.forEach((repertory, dataFileList) -> {
			File repFile = new File(destFolderFullPath + File.separator + repertory);
			if (!repFile.exists()) {
				repFile.mkdir();
			}
			dataFileList.forEach(dataFile -> {
				Path source = Paths.get(dataFile.getAbsolutePath());
				Path destination = Paths.get(repFile.getAbsolutePath() + File.separator + dataFile.getName());
				try {
					Files.copy(source, destination);
				} catch (IOException e) {
					logger.error("Problem while copying parameter data file \"" + source.toString() + "\" into folder \"" + destination.toString()
							+ "\" :\n" + e);
				}
			});
		});
	}

	public void createConfigZip() throws IOException {
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");

		Path zipfile = Paths.get(nlProjectFolder, "config.zip");
		URI uri = URI.create("jar:" + zipfile.toUri());

		try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
			Stream.of(ConfigFiles.values()).forEach(configFile -> {
				Path filePath = Paths.get(nlProjectFolder, configFile.fileName);
				Path filePathInZipfile = zipfs.getPath("/" + configFile.fileName);
				// copy a file into the zip file
				try {
					Files.copy(filePath, filePathInZipfile, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					logger.error("Error writing file in config.zip file", e);
				}
			});
		}
	}

	private void deleteConfigFiles() {
		// remove the files on disk
		Stream.of(ConfigFiles.values()).forEach(configFile -> {
			try {
				Files.delete(Paths.get(nlProjectFolder, configFile.fileName));
			} catch (IOException e) {
				logger.warn("Warning some config files were not cleanup", e);
			}
		});
	}

	public void writeNLP(String name) throws IOException {
		File nlp = new File(nlProjectFolder, name + ".nlp");
		if (!nlp.exists() && !nlp.createNewFile()) {
			logger.error("Error, cannot create the NLP file");
		}
		if (!nlp.isFile()) {
			throw new IllegalArgumentException("NLP file must be a file");
		}
		PropertiesConfiguration nlpProperties = new PropertiesConfiguration();
		nlpProperties.getLayout().setGlobalSeparator("=");
		nlpProperties.addProperty("project.name", name);
		nlpProperties.addProperty("project.version", PROJECT_VERSION);
		nlpProperties.addProperty("project.original.version", PROJECT_VERSION);
		nlpProperties.addProperty("product.name", "NeoLoad");
		nlpProperties.addProperty("product.original.name", "NeoLoad");
		nlpProperties.addProperty("product.version", PRODUCT_VERSION);
		nlpProperties.addProperty("product.original.version", PRODUCT_VERSION);
		nlpProperties.addProperty("project.id", UUID.randomUUID().toString());
		nlpProperties.addProperty("project.config.path", "config.zip");
		nlpProperties.addProperty("project.config.storage", "ZIP");
		nlpProperties.addProperty("team.server.enabled", "false");

		try (final Writer out = new FileWriter(nlp)) {
			nlpProperties.setHeader("Project description file");
			nlpProperties.save(out);
		} catch (ConfigurationException e) {
			logger.error("Error while saving NLP file", e);
		}
	}
	
	public File getNlProjectFolder() {
		return new File(nlProjectFolder);
	}
}