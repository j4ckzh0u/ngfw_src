/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;
import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.UnmarshallException;

import com.untangle.uvm.util.IOUtil;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SettingsChangesEvent;
import com.untangle.uvm.app.HostnameLookup;

public class SettingsManagerImpl implements SettingsManager
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Valid characters for settings file names
     */ 
    public static final Pattern VALID_CHARACTERS = Pattern.compile("^[a-zA-Z0-9_\\-\\.\\+@]+$");

    /**
     * The extension on the filename (usually .js)
     */ 
    public static final Pattern FILE_EXTENSION = Pattern.compile("\\.\\w+$");
    
    /**
     * Match for filename with leading directory
     */ 
    public static final Pattern FILE_MATCH = Pattern.compile("(.+)\\/([a-zA-Z0-9_\\-\\+@]+)\\.\\w+\\-version\\-([0-9\\-\\.]+)\\.\\w+$");
    
    /**
     * Formatting for the version string (yyyy-mm-dd-hhmm)
     */
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd-HHmmss.SSS");

    public static final int MAX_DIFF_SIZE = 5242880;

    /**
     * This is the actual JSON serializer
     */
    private JSONSerializer serializer = null;

    /**
     * This is the locks for the various files to guarantee synchronous file access
     */
    private final Map<String, Object> pathLocks = new HashMap<String, Object>();

    protected SettingsManagerImpl()
    {
    }

    public void run13Conversion()
    {
        // 13.0 conversion
        if ( java.nio.file.Files.isDirectory(java.nio.file.Paths.get(System.getProperty("uvm.settings.dir") + "/untangle-node-shield")) ||
             java.nio.file.Files.isDirectory(java.nio.file.Paths.get(System.getProperty("uvm.settings.dir") + "/untangle-casing-http")) ||
             java.nio.file.Files.isDirectory(java.nio.file.Paths.get(System.getProperty("uvm.settings.dir") + "/untangle-node-reports")) ||
             java.nio.file.Files.isDirectory(java.nio.file.Paths.get(System.getProperty("uvm.settings.dir") + "/untangle-node-firewall")) ) {

        logger.warn("==================================");
        logger.warn("==================================");
        logger.warn("Converting old directory names ...");
        logger.warn("==================================");
        logger.warn("==================================");
            
            logger.warn("Converting old directory names ...");
            
            String[] cmds  = { "/usr/bin/rename 's/untangle-node-//' " + System.getProperty("uvm.settings.dir") + "/*",
                               "/usr/bin/rename 's/untangle-casing-//' " + System.getProperty("uvm.settings.dir") + "/*",
                               "service apache2 restart",

                               "/bin/sed " +
                               "-e 's/NodeManager/AppManager/g' " +
                               "-e 's/\"nodes\"/\"apps\"/g' " +
                               "-e 's/\"nextNodeId\"/\"nextNodeId\"/g' " +
                               "-e 's/NodeSettings/AppSettings/g' " +
                               "-e 's/nodeName/appName/g' " +
                               "-i " + System.getProperty("uvm.settings.dir") + "/untangle-vm/nodes*",

                               "/bin/sed " +
                               "-e 's/SmtpNodeSettings/SmtpSettings/g' " +
                               "-i " + System.getProperty("uvm.settings.dir") + "/smtp/settings*",

                               // just rename the symlink - not the destination
                               "/usr/bin/rename 's/nodes.js/apps.js/' " + System.getProperty("uvm.settings.dir") + "/untangle-vm/nodes.js",
            };

            for ( String command : cmds ) {
                logger.warn("Converting old directory names: " + command);
                
                ExecManagerResult result = UvmContextFactory.context().execManager().exec( command );
                try {
                    String lines[] = result.getOutput().split("\\r?\\n");
                    for ( String line : lines )
                        logger.info( "conversion: " + line );
                } catch (Exception e) {}
            }

            logger.warn("==================================");
            logger.warn("==================================");
            logger.warn("Converted old directory names .   ");
            logger.warn("==================================");
            logger.warn("==================================");
            
        }
    }
    
    /**
     * Documented in SettingsManager.java
     */
    public <T> T load( Class<T> clz, String fileName ) throws SettingsException
    {
        if (!_checkLegalName(fileName)) {
            throw new IllegalArgumentException("Invalid file name: '" + fileName + "'");
        }

        return _loadImpl(clz, fileName);
    }

    /**
     * Documented in SettingsManager.java
     */
    public <T> T loadUrl( Class<T> clz, String urlStr ) throws SettingsException
    {
        InputStream is = null;

        CloseableHttpClient httpClient = HttpClients.custom().build();
        CloseableHttpResponse response = null;
        
        try {
            URL url = new URL(urlStr);
            logger.debug("Fetching Settings from URL: " + url); 

            HttpGet get = new HttpGet(url.toString());
            get.addHeader("Accept-Encoding", "gzip");
            response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            if ( entity == null ) {
                throw new IllegalArgumentException("Invalid Response: " + entity);
            }
            is = entity.getContent();

            Object lock = this.getLock(urlStr);
            synchronized(lock) {
                return _loadInputStream(clz, is);
            }
        }
        catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: '" + urlStr + "'", e);
        }
        catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid content in URL: '" + urlStr + "'", e);
        } finally {
            try { if ( response != null ) response.close(); } catch (Exception e) { logger.warn("close",e); }
            try { httpClient.close(); } catch (Exception e) { logger.warn("close",e); }
        }
    }

    /**
     * Documented in SettingsManager.java
     */
    public String getDiff(String fileName) throws SettingsException
    {
        String diff;

        Matcher m = FILE_MATCH.matcher(fileName);
        if (!m.find()){
            throw new SettingsException("Invalid file: " + fileName);
        }
        String directoryName = m.group(1);
        final String baseName = m.group(2);
        String currentVersion = m.group(3);

        /**
         * Walk directory to get most recent file with our base that is not us.
         */
        // 
        File currentFile = new File(fileName);
        final long currentFileLastModified = currentFile.lastModified();

        File directory = new File(directoryName);
        File[] directoryListing = directory.listFiles();
        File previousFile = null;
        if(directoryListing != null){
            for(File df: directoryListing){
                if(df.getName().startsWith(baseName) == false){
                    continue;
                }
                if((df.lastModified() < currentFileLastModified) &&
                    ((previousFile == null) || 
                     (previousFile.lastModified() < df.lastModified()))
                ){
                    previousFile = df;
                }
            }
        }

        if( previousFile == null ){
            throw new SettingsException("Could not find an earlier file to compare against.");            
        }

        if( previousFile.length() > MAX_DIFF_SIZE || currentFile.length() > MAX_DIFF_SIZE ){
            /**
             * Current IDPS settings diff will consume so much memory that freeing that
             * memory will trigger a garbage collection problem that will bring down the vm
             */
            throw new SettingsException("Settings are too big to compare");
        }

        String command = "diff -y -W1024 -t " + previousFile.getAbsolutePath() + " " + fileName;
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(command);
        if ( result.getResult() > 1) {
            throw new SettingsException("Bad result on diff");
        }else{
            diff = result.getOutput();
        }
        return diff;
    }

    /**
     * Documented in SettingsManager.java
     */
    public void save( String fileName, Object value ) throws SettingsException
    {
        save( fileName, value, true );
    }
    
    public void save( String fileName, Object value, boolean saveVersion ) throws SettingsException
    {
        if (!_checkLegalName(fileName)) {
            throw new IllegalArgumentException("Invalid file name: '" + fileName + "'");
        }

        _saveImpl( fileName, value, saveVersion, true );
    }

    public void save( String fileName, Object value, boolean saveVersion, boolean prettyFormat ) throws SettingsException
    {
        if (!_checkLegalName(fileName)) {
            throw new IllegalArgumentException("Invalid file name: '" + fileName + "'");
        }

        _saveImpl( fileName, value, saveVersion, prettyFormat );
    }
    
    public void save( String fileName, String inputFilename, boolean saveVersion ) throws SettingsException
    {
        if (!_checkLegalName(fileName)) {
            throw new IllegalArgumentException("Invalid file name: '" + fileName + "'");
        }

        _saveImpl( fileName, inputFilename, saveVersion, true );
    }

    /**
     * @param serializer
     *            the serializer to set
     */
    protected void setSerializer( JSONSerializer serializer )
    {
        this.serializer = serializer;
    }

    /**
     * @return the serializer
     */
    protected JSONSerializer getSerializer()
    {
        return serializer;
    }
    
    /**
     * Implementation of the load
     * This opens the file, and then calls loadInputStream
     */
    private <T> T _loadImpl( Class<T> clz, String fileName ) throws SettingsException
    {
        File f = new File( fileName );
        if (!f.exists()) {
            return null;
        }

        InputStream is = null;
        try {
            is = new FileInputStream(f);
        }
        catch (java.io.FileNotFoundException e) {
            throw new SettingsException("File not found: " + f);
        }

        Object lock = this.getLock(f.getParentFile().getAbsolutePath());
        synchronized(lock) {
            return _loadInputStream(clz, is);
        }
    }

    /**
     * Implementation of the load using a stream
     */
    @SuppressWarnings("unchecked") //JSON
    private <T> T _loadInputStream( Class<T> clz, InputStream is ) throws SettingsException
    {
        BufferedReader reader = null;
        try {
            StringBuilder jsonString = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line+"\n");
            }

            logger.debug("Loading Settings: \n" + "-----------------------------\n" + jsonString + "-----------------------------\n");

            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return (T) serializer.fromJSON(jsonString.toString());
        } catch (IOException e) {
            logger.warn("IOException: ",e);
            throw new SettingsException("Unable to the settings: '" + is + "'", e);
        } catch (UnmarshallException e) {
            logger.warn("UnmarshallException: ",e);
            for ( Throwable cause = e.getCause() ; cause != null ; cause = cause.getCause() ) {
                logger.warn("Exception cause: ", cause);
            }
            throw new SettingsException("Unable to unmarshal the settings: '" + is + "'", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {}
        }
    }
    
    /**
     * Implementation of the save
     *
     * This serializes the JSON object to a tmp file
     * Then formats that tmp file and copies it to another file
     * Then it repoints the symlink
     */
    private void _saveImpl( String fileName, Object value, boolean saveVersion, boolean prettyPrint ) throws SettingsException
    {
        String outputFileName = _getVersionedFileName( fileName, saveVersion );
        File outputFile = new File(outputFileName);

        Object lock = this.getLock(outputFile.getParentFile().getAbsolutePath());

        /*
         * Synchronized on the name of the parent directory, so two files cannot
         * modify the same file at the same time
         */
        synchronized (lock) {
            FileWriter fileWriter = null;

            try {
                File parentFile = outputFile.getParentFile();

                /* Create the directory structure */
                parentFile.mkdirs();

                fileWriter = new FileWriter(outputFile);
                String json = this.serializer.toJSON(value);
                logger.debug("Saving Settings: \n" + json);
                fileWriter.write(json);
                fileWriter.close();
                fileWriter = null;

                _saveCommit( fileName, outputFileName, saveVersion, prettyPrint );

            } catch (IOException e) {
                logger.warn("Failed to save settings: ", e);
                throw new SettingsException("Unable to save the file: '" + fileName + "'", e);
            } catch (MarshallException e) {
                logger.warn("Failed to save settings: ", e);
                for ( Throwable cause = e.getCause() ; cause != null ; cause = cause.getCause() ) {
                    logger.warn("Exception cause: ", cause);
                }
                throw new SettingsException("Unable to marshal json string:", e);
            } finally {
                try {
                    if (fileWriter != null) {
                        fileWriter.close();
                    }
                } catch (Exception e) {}

                // call settings change hook
                UvmContextFactory.context().hookManager().callCallbacks( HookManager.UVM_SETTINGS_CHANGE, outputFileName );
            }
            return;
        }
    }

    /**
     * Implementation of the save from an existing file, not a json object.
     *
     * This serializes the JSON object to a tmp file
     * Then formats that tmp file and copies it to another file
     * Then it repoints the symlink
     */
    private void _saveImpl( String fileName, String inputFileName, boolean saveVersion, boolean prettyFormat ) throws SettingsException
    {

        String outputFileName = _getVersionedFileName( fileName, saveVersion );

        File inputFile = new File(inputFileName);
        File outputFile = new File(outputFileName);
        Object lock = this.getLock(outputFile.getParentFile().getAbsolutePath());

        /*
         * Synchronized on the name of the parent directory, so two files cannot
         * modify the same file at the same time
         */
        synchronized (lock) {
            try{
                IOUtil.copyFile( inputFile, outputFile );
            } catch (IOException e) {
                logger.warn("Failed to save settings: ", e);
                throw new SettingsException("Unable to copy the file: '" + inputFileName + "' to '" + outputFileName + "'", e);
            }finally{
                IOUtil.delete( inputFile );

                _saveCommit( fileName, outputFileName, saveVersion, prettyFormat );
            }
        }
    }

    /**
     * Reformat the settings file and create its symlink.
     *
     * @param fileName
     *          Live filename.
     * @param outputFileName
     *          Versioned filename.
     * @param saveVersion
     *          If true, create symlink
     */
    private void _saveCommit( String fileName, String outputFileName, boolean saveVersion, boolean prettyFormat )
    {
        File link = new File( fileName );
        logger.info("Wrote: " + outputFileName);
        
        if ( prettyFormat ) {
            String formatCmd = new String(System.getProperty("uvm.bin.dir") + "/" + "ut-format-json" + " " + outputFileName );
            UvmContextImpl.context().execManager().setLevel(org.apache.log4j.Level.DEBUG);
            UvmContextImpl.context().execManager().execResult(formatCmd);
            UvmContextImpl.context().execManager().setLevel(org.apache.log4j.Level.INFO);
        }

        /**
         * Call sync to force save to filesystem
         */
        UvmContextImpl.context().execManager().setLevel(org.apache.log4j.Level.DEBUG);
        UvmContextImpl.context().execManager().execResult("sync");
        UvmContextImpl.context().execManager().setLevel(org.apache.log4j.Level.INFO);
        
        if ( saveVersion ) {
            String[] chops = outputFileName.split(File.separator);
            String filename = chops[chops.length - 1];

            try {
                Path target = FileSystems.getDefault().getPath( "./" + filename );
                Path symlink = FileSystems.getDefault().getPath( link.toString() );
                Files.deleteIfExists( symlink );
                Files.createSymbolicLink( symlink, target );
            
                String username = null;
                String hostname = null;
                if((UvmContextImpl.getInstance().threadRequest() != null) &&
                   (UvmContextImpl.getInstance().threadRequest().get() != null)){
                    username = UvmContextImpl.getInstance().threadRequest().get().getRemoteUser();
                    HostnameLookup reports = (HostnameLookup) UvmContextFactory.context().appManager().app("reports");
                    hostname = UvmContextImpl.getInstance().threadRequest().get().getRemoteAddr();
                    if( reports != null && hostname != null){
                        hostname = reports.lookupHostname(InetAddress.getByName(UvmContextImpl.getInstance().threadRequest().get().getRemoteAddr()));
                    }
                    if( hostname == null ){
                        hostname = UvmContextImpl.getInstance().threadRequest().get().getRemoteAddr();
                    }
                }
                UvmContextFactory.context().logEvent(new SettingsChangesEvent(outputFileName, username, hostname));
            } catch ( Exception e ) {
                logger.warn( "Failed to create symbolic link.", e );
            }

            //old way
            //String linkCmd = "ln -sf ./"+filename + " " + link.toString();
            //UvmContextImpl.context().execManager().exec(linkCmd);
        }
        
    }

    /**
     * Retrieve a lock to use for a given path. By locking on an object you
     * guarantee the object is unique.
     * http://www.javalobby.org/java/forums/t96352.html
     * 
     * @param lockName
     *            The name of the lock
     * @return An object that can be used to lock on path.
     */
    private Object getLock(String lockName)
    {
        Object lock = this.pathLocks.get(lockName);
        if (lock == null) {
            lock = new Object();
            this.pathLocks.put(lockName, lock);
        }

        return lock;
    }

    /**
     * Check if a filename is "legal"
     * Must have valid characterns and an extension
     */
    private boolean _checkLegalName(String name) throws IllegalArgumentException
    {
        if (!VALID_CHARACTERS.matcher( name.replace("/","") ).matches()) {
            logger.error("Illegal name (Invalid characters): " + name);
            return false;
        }

        Matcher m = FILE_EXTENSION.matcher( name );
        if ( ! m.find()) {
            logger.error("Illegal name (Missing file extension): " + name);
            return false;
        }

        if ( name.contains("../") ) {
            logger.error("Illegal name (contains ../): " + name);
            return false;
        }
            
        return true;
    }

    /**
     * Finds the file extension and returns it as a string
     * Example fileName = foo.js returns ".js"
     */
    private String _findFileExtension( String fileName )
    {
        Matcher m = FILE_EXTENSION.matcher( fileName );
        if ( m.find() ) {
            return m.group();
        }

        return null;
    }

    /**
     * From the specified base filename, create a versioned name.
     *
     * @param filename
     *          Settings file name.
     * @param saveVersion
     *          Indiciates whether to generate a versioned file name.
     */
    private String _getVersionedFileName( String fileName, boolean saveVersion )
    {
        String outputFileName;
        if (saveVersion){
            String versionString = String.valueOf(DATE_FORMATTER.format(new Date()));
            outputFileName = fileName + "-version-" + versionString + _findFileExtension( fileName );
        } else {
            outputFileName = fileName;
        }
        return outputFileName;
    }

}
