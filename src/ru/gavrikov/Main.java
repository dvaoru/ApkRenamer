package ru.gavrikov;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
        // java -jar -in /home/sasha/IdeaProjects/renamer/test/92.apk -out /home/sasha/IdeaProjects/renamer/test/92mod.apk -icon /home/sasha/Downloads/Renamer_jar/icon/Network-Download-icon.png
        ArrayList<String> arguments = new ArrayList(Arrays.asList(args));
        if (arguments.indexOf("-h") != -1){
            showHelp();
            return;
        }
        String in = getArg(arguments, "-a");
        String out = getArg(arguments, "-o");
        String name = getArg(arguments, "-n");
        String pack = getArg(arguments, "-p");
        String icon = getArg(arguments, "-i");

        System.out.println(in);
        System.out.println(out);
        System.out.println(icon);

        Renamer mRenamer;
        if (args.length > 0) {
            mRenamer = new Renamer(in, out, name, pack, icon);
        }else{
            mRenamer = new Renamer();
        }
        mRenamer.run();

    }

    private static void showHelp() {
        String helpText =
                "\n" +
                "Use the renamer program to change an app name, a package name and an icon in an Android app.\n" +
                "\n" +
                "Usage: java -jar renamer.jar [-a path/to/app.apk] [-o path/to/renamed_app.apk] [-n new_name] [-p new.package.name] [-i new_icon.png] \n" +
                "\n" +
                "You can place app.apk to \"in\" folder, new_icon.png to \"icon\" folder \n" +
                "and run java -jar renamer.jar without arguments. Your renamed_app.apk will be placed in \"out\" folder";
        System.out.println(helpText);
    }

    private static String getArg(ArrayList<String> al, String argName) {
        String result = "";
        int index = al.indexOf(argName);
        if (index != -1) {
            result = al.get((index + 1));
        }

        return result;
    }
}


class Renamer {
    private File inApk = null;
    private File outApk = null;
    private File iconFile = null;


    private String appName = "";
    private String pacName = "";
    private String iconName = "";


    Renamer() throws IOException {
        appName = inputNewName();
        pacName = inputNewPackageName();
    }

    public Renamer(String in, String out, String name, String pack, String icon) {
        if (!in.equals("")){
            this.inApk = new File(in);
        }
        if (!out.equals("")){
            this.outApk = new File(out);
        }
        if (!name.equals("")){
            this.appName = name;
        }
        if (!pack.equals("")){
            this.pacName = pack;
        }
        if (!icon.equals("")){
            this.iconFile = new File(icon);
        }

    }

    void run(){
        delTempDir();
        extractApk();
        modifySources();
        buildApk();
        signApk();
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

    private void runJar(File file, String[] args) {
        final String mainClass;
        final JarFile jarFile;
        try {
            jarFile = new JarFile(file);
            try {
                final Manifest manifest = jarFile.getManifest();
                mainClass = manifest.getMainAttributes().getValue("Main-Class");
            } finally {
                jarFile.close();
            }
            final URLClassLoader child = new URLClassLoader(new URL[]{file.toURI().toURL()}, this.getClass().getClassLoader());
            final Class classToLoad = Class.forName(mainClass, true, child);
            final Method method = classToLoad.getDeclaredMethod("main", String[].class);
            final Object[] arguments = {args};
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            method.invoke(null, arguments);
        } catch (IOException | ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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
        if (this.outApk == null){
            outApk = new File(getOutDir() + File.separator + pacName + ".apk");
        }else{
            if (this.outApk.isDirectory()){
                this.outApk = new File (this.outApk + File.separator + pacName + ".apk");
            }
        }
        return this.outApk;
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

    private File getSignApkJar() {
        return new File(getBinDir() + File.separator + "signapk.jar");
    }

    private File getManifestFile() {
        return new File(getTempDir() + File.separator + "AndroidManifest.xml");
    }


    private void extractApk()  {
        l(" d " + getSubjectApk().toString() + " -f " + " -o " + getTempDir().toString());
        runJar(getApktoolJar(), new String[]{"d", getSubjectApk().toString(), "-f", "-o", getTempDir().toString()});
    }

    private void buildApk() {
        runJar(getApktoolJar(), new String[]{"b", getTempDir().toString(), "-o", getUnsignedApk().toString()});
    }

    private void signApk()  {
        l("signed apk " + getSignedApk().toString());
        runJar(getSignApkJar(), new String[]{getPemKey().toString(), getPk8Key().toString(),
                getUnsignedApk().toString(), getSignedApk().toString()});
        getUnsignedApk().delete();
    }

    private void changePackageName(Node n, String packageName) {
        replaceAttribute(n, new String[]{}, "package", packageName);
    }

    private Node getMainXmlNode(File f) {
        Node root = null;
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

    private void writeFile(File file, String text) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            l("Error file: " + file);
            l(e.toString());
        }
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
        if (!this.pacName.equals("")) {
            changePackageName(manifest, this.pacName);
            l(manifest.toString());
        }

        changeStrings(manifest);

        File newIcon = getIconPng();
        if (newIcon != null) {
            changeImages(manifest, newIcon);
            changeIconName(manifest);
        }
        saveXmlFile(getManifestFile(), manifest);
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

    private String getNewIconName(){
        if (this.iconName.equals("")){
            this.iconName = generateString(new Random(), "abcdefghijklmnopqrstuvwxyz", 10);
        }
        return  this.iconName;
    }

    private static String generateString(Random rng, String characters, int length)
    {
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }

    private void sendNewIcon(File newIcon, File minmapDir, String newIconName) {
        int size = getDpi(minmapDir);
        String command = "" + getSjitJar() + " -in " + newIcon + " -resize " + size + "px -out " +
                minmapDir + File.separator + newIconName + ".png";
        l(command);
        runJar(getSjitJar(), new String[]{"-in", newIcon.toString(), "-resize", size + "px", "-out",
                minmapDir + File.separator + newIconName + ".png"});
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


}