package games.strategy.engine;

import static games.strategy.engine.config.client.GameEnginePropertyReader.GAME_ENGINE_PROPERTIES_FILE;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.logging.Level;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.io.FileUtils;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import lombok.extern.java.Log;

/**
 * Provides methods to work with common file locations in a client installation.
 */
@Log
public final class ClientFileSystemHelper {
  private ClientFileSystemHelper() {}

  /**
   * Returns top-most, or the root folder for the TripleA installation folder.
   *
   * @return Folder that is the 'root' of the tripleA binary installation. This folder and
   *         contents contains the versioned content downloaded and initially installed. This is
   *         in contrast to the user root folder that is not replaced between installations.
   *
   * @throws IllegalStateException If the root folder cannot be located.
   */
  public static File getRootFolder() {
    try {
      return getFolderContainingFileWithName(GAME_ENGINE_PROPERTIES_FILE, getCodeSourceFolder());
    } catch (final IOException e) {
      throw new IllegalStateException("unable to locate root folder", e);
    }
  }

  private static File getCodeSourceFolder() throws IOException {
    final @Nullable CodeSource codeSource = ClientFileSystemHelper.class.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      throw new IOException("code source is not available");
    }

    final File codeSourceLocation;
    try {
      codeSourceLocation = new File(codeSource.getLocation().toURI());
    } catch (final URISyntaxException e) {
      throw new IOException("code source location URI is malformed", e);
    }

    // code source location is either a jar file (installation) or a folder (dev environment)
    return codeSourceLocation.isFile() ? codeSourceLocation.getParentFile() : codeSourceLocation;
  }

  @VisibleForTesting
  static File getFolderContainingFileWithName(final String fileName, final File startFolder) throws IOException {
    return getFolderContainingFileWithName(fileName, startFolder, startFolder);
  }

  private static File getFolderContainingFileWithName(
      final String fileName,
      final File startFolder,
      final @Nullable File currentFolder)
      throws IOException {
    if (currentFolder == null) {
      throw new IOException(String.format(
          "unable to locate file with name '%s' starting from folder '%s'",
          fileName, startFolder.getAbsolutePath()));
    }

    final boolean currentFolderContainsFileWithName = FileUtils.listFiles(currentFolder).stream()
        .filter(File::isFile)
        .map(File::getName)
        .anyMatch(fileName::equals);
    return currentFolderContainsFileWithName
        ? currentFolder
        : getFolderContainingFileWithName(fileName, startFolder, currentFolder.getParentFile());
  }

  /**
   * TripleA stores two folders, one for user content that survives between game installs,
   * and a second that contains binaries. This method returns the 'user folder', which contains
   * maps and save games.
   *
   * @return Folder where tripleA 'user data' is stored between game installations. This folder
   *         would contain as some examples: save games, downloaded maps. This location is currently
   *         not configurable (ideally we would allow this to be set during install perhaps).
   */
  public static File getUserRootFolder() {
    final File userHome = new File(SystemProperties.getUserHome());
    final File rootDir = new File(new File(userHome, "Documents"), "triplea");
    return rootDir.exists() ? rootDir : new File(userHome, "triplea");
  }

  /**
   * Returns location of the folder containing downloaded TripleA maps. The folder will be created if it does not exist.
   *
   * @return Folder where maps are downloaded and stored. Default location is relative
   *         to users home folder and not the engine install folder, this allows it to be
   *         retained between engine installations. Users can override this location in settings.
   */
  public static File getUserMapsFolder() {
    final String path = getUserMapsFolderPath(ClientSetting.USER_MAPS_FOLDER_PATH, ClientSetting.MAP_FOLDER_OVERRIDE);
    final File mapsFolder = new File(path);
    if (!mapsFolder.exists()) {
      mapsFolder.mkdirs();
    }
    if (!mapsFolder.exists()) {
      log.log(Level.SEVERE, "Error, downloaded maps folder does not exist: " + mapsFolder.getAbsolutePath());
    }
    return mapsFolder;
  }

  @VisibleForTesting
  static String getUserMapsFolderPath(
      final GameSetting currentUserMapsFolderPathSetting,
      final GameSetting overrideUserMapsFolderPathSetting) {
    return overrideUserMapsFolderPathSetting.isSet()
        ? overrideUserMapsFolderPathSetting.value()
        : currentUserMapsFolderPathSetting.value();
  }

  /** Create a temporary file, checked exceptions are re-thrown as unchecked. */
  public static File createTempFile() {
    try {
      return File.createTempFile("triplea", "tmp");
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to create a temporary file", e);
    }
  }
}
