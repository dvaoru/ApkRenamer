package ru.gavrikov;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


import org.apache.commons.io.FileUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Main {

    public static void main(String[] args) throws IOException {

        ArrayList<String> arguments = new ArrayList(Arrays.asList(args));
        if (arguments.indexOf("-h") != -1) {
            showHelp();
            return;
        }

        ArgumentsParser parser = new ArgumentsParser(arguments);
        String in = parser.extractArgument("-a");
        String out = parser.extractArgument("-o");
        String name = parser.extractArgument("-n");
        String pack = parser.extractArgument("-p");
        String icon = parser.extractArgument("-i");
        String dictionary = parser.extractArgument("-r");
        String decodeArguments = parser.extractArgument("-da");
        String buildArguments = parser.extractArgument("-ba");


        Boolean isDeepRename = parser.extractBooleanArgument("-d");
        Boolean isPauseActive = parser.extractBooleanArgument("-t");
        Boolean isSkipModify = parser.extractBooleanArgument("-m");

        System.out.println(in);
        System.out.println(out);
        System.out.println(icon);


        Renamer mRenamer = new Renamer(in, out, name, pack, icon, isDeepRename, isPauseActive, isSkipModify, decodeArguments, buildArguments, dictionary);
//        if (args.length > 1) {
//            mRenamer = new Renamer(in, out, name, pack, icon, isDeepRename, isPauseActive, isSkipModify);
//        } else {
//            mRenamer = new Renamer(isDeepRename, isPauseActive, isSkipModify);
//        }

        mRenamer.run();

    }

    private static void showHelp() {
        String helpText = "\n" + "Use the renamer program to change an app name, a package name and an icon in an Android app.\n"
                + "\n" + "Usage: java -jar renamer.jar [-a path/to/app.apk] [-o path/to/renamed_app.apk] [-n new_name] [-p new.package.name] [-i new_icon.png] \n"
                + "\n" + "You can place app.apk to \"in\" folder, new_icon.png to \"icon\" folder \n"
                + "and run java -jar renamer.jar without arguments. Your renamed_app.apk will be placed in \"out\" folder"
                + "\n\nAdd the [-d] flag to perform a \"deep renaming\"."
                + "\n This will search for instances of the old package name in all files and replace them with the new package name."
                + "\n Note that the deep renaming may cause unintended side effects, such as breaking the app functionality."
                + "\n\nAdd the [-t] flag and the program extract all apk resources at \"temp\" folder where you can modify it as you want."
                + "\n After you made the changes you can resume program flow and it builds and signs the renamed apk"
                + "\n\nAdd the [-m] flag and the program will not modify the resources of the apk."
                + "\n It extracts the apk resources to \"temp\" folder where you can modify what you want manually."
                + "\n The program will not rename anything. After you made changes resume the program and it builds and signs the package."
                + "\n\nAdd the [-da \"-option1 -option2\"] to pass arguments to Apktool when it decodes the apk."
                + "\nAdd the [-ba \"-option1 -option2\"] to pass arguments to Apktool when it builds the apk."
                + "\nThe string with arguments for Apktool should be enclosed in quotation marks."
                + "\n\nAdd the [-r] <path/to/dictionary.txt> flag and the program will replace text in APK files using a dictionary.";
        System.out.println(helpText);
    }

}

class ArgumentsParser {
    ArrayList<String> arguments;

    ArgumentsParser(ArrayList<String> args) {
        arguments = args;
    }

    String extractArgument(String argName) {
        String result = "";
        int index = arguments.indexOf(argName);
        if (index != -1) {
            result = arguments.get((index + 1));
            arguments.remove(index);
            arguments.remove(index);
        }
        return result;
    }

    Boolean extractBooleanArgument(String argName) {
        Boolean result = false;
        int index = arguments.indexOf(argName);
        if (index != -1) {
            result = true;
            arguments.remove(index);
        }
        return result;
    }
}

class Renamer {
    private File inApk = null;
    private File outApk = null;
    private File iconFile = null;


    private String appName = "";
    private String pacName = "out";
    private String iconName = "";

    private Boolean isDeepRenaming = false;
    private Boolean isPauseActive = false;
    private Boolean isSkipModify = false;

    private String decodeArguments = "";
    private String buildArguments = "";

    private String dictionaryPath = "";


//    Renamer(String in, String out, String name, String pack, String icon, Boolean isDeepRenaming, Boolean isPauseActive, Boolean isSkipModify) {
//        if (!isSkipModify) {
//            this.appName = inputNewName();
//            this.pacName = inputNewPackageName();
//        }
//        this.isDeepRenaming = isDeepRenaming;
//        this.isPauseActive = isPauseActive;
//        this.isSkipModify = isSkipModify;
//    }

    public Renamer(String in,
                   String out,
                   String name,
                   String pack,
                   String icon,
                   Boolean isDeepRenaming,
                   Boolean isPauseActive,
                   Boolean isSkipModify,
                   String decodeArguments,
                   String buildArguments,
                   String dictionaryPath) {
        if (!in.equals("")) {
            this.inApk = new File(in);
        }
        if (!out.equals("")) {
            this.outApk = new File(out);
        }
        if (!isSkipModify) {
            if (!name.equals("")) {
                this.appName = name;
            } else {
                this.appName = inputNewName();
            }
            if (!pack.equals("")) {
                this.pacName = pack;
            } else {
                this.pacName = inputNewPackageName();
            }
        }
        if (!icon.equals("")) {
            this.iconFile = new File(icon);
        }

        this.decodeArguments = decodeArguments;
        this.buildArguments = buildArguments;

        this.isDeepRenaming = isDeepRenaming;
        this.isPauseActive = isPauseActive;
        this.isSkipModify = isSkipModify;

        this.dictionaryPath = dictionaryPath;

    }

    void run() {
        delTempDir();
        extractApk();
        if (!isSkipModify) modifySources();
        if (isPauseActive || isSkipModify) makePause();
        if (!dictionaryPath.equals("")) replaceViaDictionary();
        buildApk();
        zipalignApk();
        signApk();
    }

    private void replaceViaDictionary() {
        Map<String, String> dictionary = getDictionary(dictionaryPath);
        ArrayList<File> filesList = getFilesList(getTempDir());

        for (File f : filesList) {
            for (String key : dictionary.keySet()) {
                try {

                    replaceBytesInFile(f, key.getBytes("UTF-8"), dictionary.get(key).getBytes("UTF-8"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    //Show a message and wait enter to proceed
    private void makePause() {
        String message = ("\nThe process of building the package on the pause." +
                "\n You can modify the app resources in \"temp\" folder." +
                "\n The \"temp\" folder: " + getTempDir() +
                "\n Press ENTER to proceed the building process.\n");
        System.out.println(message);
        Scanner in = new Scanner(System.in);
        in.nextLine();
    }


    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    private static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    private void runExec(File file, String[] args) {
        String[] command = new String[args.length + 1];
        command[0] = file.toString();
        for (int i = 1; i < command.length; i++) {
            command[i] = args[i - 1];
        }
        String logMessage = "RunExe: ";
        for (String c : command) {
            logMessage += c + " ";
        }
        l(logMessage);

        try {
            Process process = Runtime.getRuntime().exec(command);
            //process.waitFor();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                l(s);
            }
            while ((s = stdError.readLine()) != null) {
                l(s);
            }
        } catch (Exception e) {
            l("Error due execution " + file);
            e.printStackTrace();
        }

    }

    public class JarExecutionException extends RuntimeException {
        public JarExecutionException(String message) {
            super(message);
        }
    }

    private void runJar(File file, String[] args) throws JarExecutionException {
        try {
            String filePath = file.getAbsolutePath();
            List<String> command = new ArrayList<String>();
            command.add("java");
            command.add("-jar");
            command.add(filePath);
            for (String arg : args) {
                command.add(arg);
            }
            String logText = "Run: ";
            for (String s : command) {
                logText += " " + s;
            }
            l(logText);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                throw new JarExecutionException("The " + file.getName() + " file exited with an error: " + exitCode);
            }
        } catch (IOException e) {
            throw new JarExecutionException("An I/O error occurred while running the " + file.getName() + " file" + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JarExecutionException("The " + file.getName() + " file execution was interrupted" + e);
        }
    }

    private void delTempDir() {
        deleteFolder(getTempDir());
    }

    public void l(String l) {
        System.out.println(l);
    }

    private String inputNewName() {
        return input("Enter a new name for the app:");
    }

    private String inputNewPackageName() {
        return input("Enter a new package name:");
    }

    private String input(String description) {
        String result;
        System.out.println(description);
        Scanner in = new Scanner(System.in);
        result = in.nextLine();
        if (result.equals("")) {
            input(description);
        }
        return result;
    }

    private String getCurrentDir() {
        return System.getProperty("user.dir");
    }

    private String getBinDir() {
        return getCurrentDir() + File.separator + "bin";
    }

    private File getInDir() {
        return new File(getCurrentDir() + File.separator + "in");
    }

    private String getOutDir() {
        return getCurrentDir() + File.separator + "out";
    }

    private File getTempDir() {
        return new File(getCurrentDir() + File.separator + "temp");
    }

    private File getCacheDir() {
        return new File(getCurrentDir() + File.separator + "cache");
    }


    private File getKeyDir() {
        return new File(getCurrentDir() + File.separator + "keys");
    }

    private File getIconDir() {
        return new File(getCurrentDir() + File.separator + "icon");
    }

    protected File getResDir() {
        return new File(getTempDir() + File.separator + "res");
    }

    private File getUnsignedApk() {
        return new File(getOutDir() + File.separator + pacName + ".unsigned.apk");
    }

    private File getSignedApk() {
        if (this.outApk == null) {
            outApk = new File(getOutDir() + File.separator + pacName + ".apk");
        } else {
            if (this.outApk.isDirectory()) {
                this.outApk = new File(this.outApk + File.separator + pacName + ".apk");
            }
        }
        return this.outApk;
    }

    private File getTempIdsigFile() {
        return new File(getSignedApk() + ".idsig");
    }

    private File getSubjectApk() {
        if (inApk == null) {
            inApk = getFile(getInDir(), ".apk");
        }
        return inApk;
    }

    private File getPk8Key() {
        return getFile(getKeyDir(), ".pk8");
    }

    private File getPemKey() {
        return getFile(getKeyDir(), ".pem");
    }

    private File getIconPng() {
        if (iconFile == null) {
            iconFile = getFile(getIconDir(), ".png");
        }
        return this.iconFile;
    }

    private HashMap<String, String> getMapsApiKey() {
        HashMap<String, String> result = new HashMap();
        File mapKeyFile = getFile(getKeyDir(), ".txt");
        if (mapKeyFile != null) {
            try {
                String name = mapKeyFile.getName().replace(".txt", "");
                BufferedReader br = new BufferedReader(new FileReader(mapKeyFile));
                String value = br.readLine();
                result.put(name, value);
            } catch (IOException e) {
            }
        }
        return result;
    }

    private File getFile(File inDir, String namePart) {
        File result = null;
        File[] apkList;
        if (namePart != null) {
            apkList = inDir.listFiles(pathname -> {
                return pathname.getName().contains(namePart);
            });
        } else {
            apkList = inDir.listFiles();
        }
        if ((apkList != null) && (apkList.length != 0)) {
            result = apkList[0];
        }
        return result;
    }

    private File getApktoolJar() {
        return new File(getBinDir() + File.separator + "apktool.jar");
    }

    private File getSjitJar() {
        return new File(getBinDir() + File.separator + "SJIT.jar");
    }


    private File getZipalignExe() {
        String osName = System.getProperty("os.name");
        File zipalignFile;
        if (osName.startsWith("Windows")) {
            zipalignFile = new File(getBinDir() + File.separator + "zipalign.exe");
        } else {
            zipalignFile = new File(getBinDir() + File.separator + "zipalign");
        }
        return zipalignFile;
    }

    private File getApksignerJar() {
        return new File(getBinDir() + File.separator + "apksigner.jar");
    }

    private File getZipalignedApk() {
        return new File(getOutDir() + File.separator + pacName + ".zipalign.apk");
    }

    private File getSignApkJar() {
        return new File(getBinDir() + File.separator + "signapk.jar");
    }

    private File getManifestFile() {
        return new File(getTempDir() + File.separator + "AndroidManifest.xml");
    }

    private Map getDictionary(String pathToDictionary) {
        List<String> allLines = new ArrayList<String>();
        Map result = new HashMap<String, String>();
        try {
            allLines = Files.readAllLines(Paths.get(pathToDictionary));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String line : allLines) {
            if (line.equals("")) continue;
            String[] pair = lineToMap(line);
            result.put(pair[0], pair[1]);
        }
        l("Dictionary: " + result.toString());
        return result;
    }

    private String[] lineToMap(String line) {
        List<Integer> splitterPosition = new ArrayList<>();
        for (int index = line.indexOf(":");
             index >= 0;
             index = line.indexOf(":", index + 1)) {
            int shieldPosition = index - 1;
            if (shieldPosition >= 0) {
                String shield = line.substring(shieldPosition, index);
                if (!shield.equals("\\")) {
                    splitterPosition.add(index);
                }
            }
        }
        if (splitterPosition.size() > 1)
            throw new RuntimeException("Dictionary file has double splitter symbol \":\" in line \'" + line + "\'");
        if (splitterPosition.size() == 0)
            throw new RuntimeException("Dictionary file has no splitter symbol \":\" in line \'" + line + "\'");
        String name = line.substring(0, splitterPosition.get(0)).replace("\\:", ":");
        String value = line.substring(splitterPosition.get(0) + 1).replace("\\:", ":");
        return new String[]{name, value};
    }


    private void extractApk() {
        String[] firstArgs = new String[]{"d", getSubjectApk().toString(), "-f", "-o", getTempDir().toString()};
        String[] additionalArgs = stringToArguments(decodeArguments);
        String[] command = concat(firstArgs, additionalArgs);
        runJar(getApktoolJar(), command);
    }

    private void buildApk() {
        String[] firstArgs = new String[]{"b", getTempDir().toString(), "-o", getUnsignedApk().toString()};
        String[] additionalArgs = stringToArguments(buildArguments);
        String[] command = concat(firstArgs, additionalArgs);

        try {
            runJar(getApktoolJar(), command);
        } catch (Renamer.JarExecutionException e) {
            try {
                l("\n!!!\nTry to fix No resource error");
                fixNoResourceError(); //Fix apktool problem with manifest
                runJar(getApktoolJar(), command);
            } catch (Renamer.JarExecutionException e1) {
                l("\n!!!\nTry to fix Invalid resource directory name error by --use-aapt2");
                String[] fixInvalidResourceDirectoryNameCommand = concat(command, new String[]{"--use-aapt2"});
                runJar(getApktoolJar(), fixInvalidResourceDirectoryNameCommand);
            }
        }
    }

    private static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private String[] stringToArguments(String s) {
        ArrayList<String> result = new ArrayList();
        String[] listArgs = s.split(" ");
        for (int i = 0; i < listArgs.length; i++) {
            String value = listArgs[i];
            if (!value.equals("")) {
                result.add(value);
            }
        }
        return result.toArray(new String[0]);
    }

    private void zipalignApk() {
        runExec(getZipalignExe(), new String[]{"-p", "-f", "-v", "4", getUnsignedApk().toString(), getZipalignedApk().toString()});
        getUnsignedApk().delete();
    }


    private void signApk() {
        String[] command = {"sign", "--key", getPk8Key().toString(), "--cert", getPemKey().toString(), "--out", getSignedApk().toString(), getZipalignedApk().toString()};
        l("Signed apk " + getSignedApk().toString());
        runJar(getApksignerJar(), command);
        getZipalignedApk().delete();
        getTempIdsigFile().delete();
        l("");
        if (getSignedApk().exists()) {
            l("Success. Path to your renamed apk: " + getSignedApk());
        } else {
            l(":-(");
            l("Rename unsuccessful.");
            l("Please email to the developer at dvaoru@gmail.com about your issue");
        }
    }

    private void changePackageName(Node n, String packageName) {
        replaceAttribute(n, new String[]{}, "package", packageName);
    }

    private Node getMainXmlNode(File f) {
        Node root = null;
        //File f = new File(getManifestFile());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(f);
            root = doc.getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
        }
        return root;
    }


    private void replaceAttribute(Node node, String[] node_path, String attribute, String newValue) {
        Node att = getAttribute(node, node_path, attribute);
        att.setNodeValue(newValue);
    }

    private Node getAttribute(Node node, String[] node_path, String attribute) {

        Node myNode = node;
        NodeList childNodes = myNode.getChildNodes();
        for (String s : node_path) {
            myNode = getChildNode(myNode, s);
        }
        NamedNodeMap attr = myNode.getAttributes();
        Node att = attr.getNamedItem(attribute);
        return att;
    }

    private Node getChildNode(Node node, String child) {
        Node result = null;
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            String name = childNodes.item(i).getNodeName();
            if (name.equals(child)) {
                result = childNodes.item(i);
                return result;
            }
        }
        return null;
    }


    private void saveXmlFile(File file, Node node) {
        // write the content into xml file
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(node);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        } catch (TransformerException e) {

        }

    }

    private String getNameLabel(Node manifest) {
        Node att = getAttribute(manifest, new String[]{"application"}, "android:label");
        String result = att.getNodeValue();
        result = result.replace("@string/", "");
        return result;
    }

    // We do all changes in unzipped sources
    private void modifySources() {
        Node manifest = getMainXmlNode(getManifestFile());

        String packageName = getPackageName(manifest);


        if (!this.pacName.equals("")) {
            changePackageName(manifest, this.pacName);
        }


        changeStrings(manifest);
        fixProviderNoName(manifest);

        File newIcon = getIconPng();
        if (newIcon != null) {
            changeImages(manifest, newIcon);
            changeIconName(manifest);
        }
        saveXmlFile(getManifestFile(), manifest);
        if (this.isDeepRenaming) {
            l("Perform deep renaming...");
            renamePackageFolders(packageName, this.pacName);
        }

    }

    private void changeImages(Node manifest, File newIcon) {
        File[] mipMapFolders = getMipMapFolders(getMipmapFolderName(manifest));
        for (File f : mipMapFolders) {
            l(f.toString() + " " + getDpi(f));
            if (!f.getName().contains("anydpi")) {
                sendNewIcon(newIcon, f, getNewIconName());
            }
        }
    }

    private void changeIconName(Node n) {
        String name = getMipmapFolderName(n);
        replaceAttribute(n, new String[]{"application"}, "android:icon", "@" + name + "/" + getNewIconName());
    }

    private String getNewIconName() {
        if (this.iconName.equals("")) {
            this.iconName = generateString(new Random(), "abcdefghijklmnopqrstuvwxyz", 10);
        }
        return this.iconName;
    }

    private static String generateString(Random rng, String characters, int length) {
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }

    private void sendNewIcon(File newIcon, File minmapDir, String newIconName) {
        int size = getDpi(minmapDir);
        String command = "" + getSjitJar() + " -in " + newIcon + " -resize " + size + "px -out " + minmapDir + File.separator + newIconName + ".png";
        l(command);
        runJar(getSjitJar(), new String[]{"-in", newIcon.toString(), "-resize", size + "px", "-out", minmapDir + File.separator + newIconName + ".png"});
    }


    private int getDpi(File mipMapFolder) {
        String name = mipMapFolder.getName();
        int result = 48;
        if (name.contains("xxxhdpi")) {
            result = 192;
        } else if (name.contains("xxhdpi")) {
            result = 144;
        } else if (name.contains("xhdpi")) {
            result = 96;
        } else if (name.contains("hdpi")) {
            result = 72;
        }
        return result;
    }


    private String getMipmapFolderName(Node manifest) {
        Node att = getAttribute(manifest, new String[]{"application"}, "android:icon");
        String result = att.getNodeValue();
        int end = result.indexOf("/");
        result = result.substring(1, end);
        return result;
    }

    private File[] getMipMapFolders(String mipMapFolderName) {
        File[] result = getResDir().listFiles(pathname -> {
            return pathname.getName().contains(mipMapFolderName);
        });
        return result;
    }

    private void changeStrings(Node manifest) {
        String name_label = getNameLabel(manifest);
        HashMap<String, String> forReplace = new HashMap<>();
        forReplace.putAll(getMapsApiKey());
        if (!this.appName.equals("")) {
            forReplace.put(name_label, this.appName);
        }

        for (File f : getStringsFiles()) {
            Node node = getMainXmlNode(f);
            replaceStrings(node, forReplace);
            saveXmlFile(f, node);
        }
    }

    //Fix problem if in manifest provider has no name
    private void fixProviderNoName(Node manifest) {

        NodeList nl = manifest.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeName().contains("queries")) {
                NodeList child = node.getChildNodes();
                for (int j = 0; j < child.getLength(); j++) {
                    Node provider = child.item(j);
                    if (provider.getNodeName() == "provider") {
                        Boolean isNameAbsent = true;
                        NamedNodeMap attr = provider.getAttributes();
                        for (int n = 0; n < attr.getLength(); n++) {
                            l(attr.item(n).getNodeName());
                            if (attr.item(n).getNodeName() == "android:name") isNameAbsent = false;
                        }
                        if (isNameAbsent) {
                            Attr nameAttribute = provider.getOwnerDocument().createAttribute("android:name");
                            nameAttribute.setValue("myname");
                            Element providerElement = (Element) provider;
                            providerElement.setAttributeNode(nameAttribute);
                            l("Repair absent name tag in provider node");
                        }
                    }
                }
            }
        }
    }


    //Fix problem with error: Error: No resource type specified (at 'value' with value '@1996685312')
    //Make updates in the manifest
    private void fixNoResourceError() {
        Node manifest = getMainXmlNode(getManifestFile());
        NodeList nl = manifest.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeName().equals("application")) {
                NodeList applicationsNodes = node.getChildNodes();
                for (int j = 0; j < applicationsNodes.getLength(); j++) {
                    Node item = applicationsNodes.item(j);
                    if (item.getNodeName().equals("meta-data")) {
                        NamedNodeMap attributes = item.getAttributes();
                        if (attributes.getLength() > 1) {
                            if (attributes.item(0).toString().contains("com.android.vending.splits")) {
                                attributes.item(1).setNodeValue("base");
                                l("value = " + attributes.item(1));
                                saveXmlFile(getManifestFile(), manifest);
                                l("Repair error: No resource type specified");
                            }
                        }
                    }
                }
            }
        }
    }

    private void replaceStrings(Node node, HashMap<String, String> forReplace) {
        NodeList nl = node.getChildNodes();
        Set keysForReplace = forReplace.keySet();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if (item.hasAttributes()) {
                String stringName = item.getAttributes().item(0).getNodeValue();
                if (keysForReplace.contains(stringName)) {
                    item.setTextContent(forReplace.get(stringName));
                }
            }
        }
    }


    private File[] getValuesFolders() {
        File res = getResDir();
        File[] valuesFolders = res.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                boolean result = false;
                if ((pathname.isDirectory()) && (pathname.getName().contains("values"))) {
                    result = true;
                }
                return result;
            }
        });
        return valuesFolders;
    }

    private ArrayList<File> getStringsFiles() {
        ArrayList<File> result = new ArrayList<>();
        File[] valuesFolders = getValuesFolders();
        for (File vf : valuesFolders) {
            File[] stringsFile = vf.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    boolean res = false;
                    if (pathname.getName().equals("strings.xml")) {
                        res = true;
                    }
                    return res;
                }
            });
            if (stringsFile != null) {
                result.addAll(Arrays.asList(stringsFile));
            }
        }
        return result;
    }

    //Replace a text in a file
    private void replaceText(File file, String toReplace, String replacement) {
        try {
            Charset charset = StandardCharsets.UTF_8;
            String content = new String(Files.readAllBytes(file.toPath()), charset);
            content = content.replaceAll(toReplace, replacement);
            Files.write(file.toPath(), content.getBytes(charset));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //Replace bytes in a file
    private void replaceBytesInFile(File file, byte[] searchBytes, byte[] replaceBytes) throws IOException {
        byte[] origin = FileUtils.readFileToByteArray(file);
        byte[] replaced = replaceBytesInArray(origin, searchBytes, replaceBytes);

        if (replaced != null) {
            l("File: " + file + " changed");
            FileUtils.delete(file);
            FileUtils.writeByteArrayToFile(file, replaced);
        }
    }

    private byte[] replaceBytesInArray(byte[] origin, byte[] searchBytes, byte[] replaceBytes) {
        List<Byte> originList = arrayToList(origin);
        List<Byte> searchList = arrayToList(searchBytes);
        List<Byte> replaceList = arrayToList(replaceBytes);
        if (searchList.equals(replaceList)) return null;
        Boolean isChanged = false;
        while (true) {
            int startPosition = Collections.indexOfSubList(originList, searchList);
            if (startPosition == -1) break;
            isChanged = true;
            for (int i = 0; i < searchBytes.length; i++) {
                originList.remove(startPosition);
            }
            originList.addAll(startPosition, replaceList);
        }
        byte[] result = new byte[originList.size()];
        for (int i = 0; i < originList.size(); i++) {
            result[i] = originList.get(i);
        }
        if (isChanged) {
            return result;
        } else {
            return null;
        }
    }

    private static List<Byte> arrayToList(byte[] arr) {
        List<Byte> result = new ArrayList<>();
        for (byte b : arr) {
            result.add(b);
        }
        return result;
    }


    //Recursive list of files in a folder
    private ArrayList<File> getFilesList(File folder) {
        ArrayList<File> result = (ArrayList<File>) FileUtils.listFiles(folder, null, true);
        return result;
    }

    //Get package name from Manifest
    private String getPackageName(Node manifest) {
        return manifest.getAttributes().getNamedItem("package").getNodeValue();
    }


    //Rename folders with smali for new package
    private void renamePackageFolders(String oldPackageName, String newPackageName) {
        List<File> smaliFolders = Arrays.asList(Objects.requireNonNull(getTempDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (pathname.isDirectory() && pathname.getName().contains("smali"));
            }
        })));

        for (File folder : smaliFolders) {
            File cache = movePackageToCache(folder, oldPackageName);
            if (cache != null) {
                File destination = moveFromCacheToPackage(folder, newPackageName);
            }
        }
        ArrayList<File> smaliFiles = getFilesList(getTempDir());
        for (File f : smaliFiles) {
            String oldPackageLabel = oldPackageName.replace(".", "/");//Lru/gavrikov/mocklocations
            String newPackageLabel = newPackageName.replace(".", "/");
            try {
                replaceBytesInFile(f, oldPackageLabel.getBytes(), newPackageLabel.getBytes());
                replaceBytesInFile(f, oldPackageName.getBytes(), newPackageName.getBytes());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


    }

    private File movePackageToCache(File smaliFolder, String packageName) {
        File sourceFolder = new File(smaliFolder.getAbsolutePath() + File.separator + packageName.replace(".", File.separator));
        if (sourceFolder.exists()) {
            try {
                File cache = getCacheDir();
                FileUtils.deleteDirectory(cache);
                cache.mkdir();
                FileUtils.copyDirectory(sourceFolder, cache);
                File oldPackageRoot = new File(smaliFolder, packageName.split("\\.")[0]);
                FileUtils.deleteDirectory(oldPackageRoot);
                return cache;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private File moveFromCacheToPackage(File smaliFolder, String packageName) {
        File cache = getCacheDir();
        File targetFolder = smaliFolder;
        for (String name : packageName.split("\\.")) {
            targetFolder = new File(targetFolder, name);
            targetFolder.mkdir();
        }
        try {
            FileUtils.copyDirectory(cache, targetFolder);
            FileUtils.deleteDirectory(cache);
            return targetFolder;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//https://forum.xda-developers.com/t/how-to-guide-mod-change-package-names-of-apks.2760965/
}