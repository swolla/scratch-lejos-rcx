import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ScratchCompiler {
    public static void main(String[] args) {
        new ScratchCompiler().run();
    }

    public void run() {
        String json = readSB3();
        Map project = (Map) read(json);
        try {
            PrintWriter java = new PrintWriter("ScratchProject.java");
            addJSONComment(java, json);
            compileClass(java, project);
            java.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addJSONComment(PrintWriter java, String json) {
        java.write("/**");
        String indent = "";
        boolean inString = false;
        char last = ' ';
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                java.write(c);
                if (c == '"' && last != '\\') {
                    inString = false;
                }
            } else {
                if (c == '{' || c == '[') {
                    indent += "    ";
                    java.write("\n * " + indent + c + ' ');
                } else if (c == ',') {
                    java.write("\n * " + indent + c + ' ');
                } else if (c == '}' || c == ']') {
                    java.write("\n * " + indent + c);
                    indent = indent.substring(4);
                } else if (c == '"') {
                    inString = true;
                    java.write(c);
                } else {
                    java.write(c);
                }
            }
            last = c;
        }
        java.write("\n */\n");
    }

    private String readSB3() {
        try {
            ZipFile zipFile = new ZipFile("Scratchproject.sb3");
            ZipEntry zipEntry = zipFile.getEntry("project.json");
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            Scanner scanner = new Scanner(inputStream, "UTF-8");
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void compileClass(PrintWriter java, Map project) {
        java.write("public final class ScratchProject extends ScratchAdapter {\n");
        List targets = getList(project, "targets");
        for (int i = 0; i < targets.size(); i++) {
            Map target = (Map) targets.get(i);
            Boolean isStage = getBoolean(target, "isStage");
            if (!isStage.booleanValue()) {
                compileMethods(java, target);
            }
        }
        // Generate statements from blocks
        //       call Motion and Control classes
        java.write("}\n");
    }

    private void compileMethods(PrintWriter java, Map target) {
        Map blocks = getMap(target, "blocks");
        Iterator iterator = blocks.values().iterator();
        while (iterator.hasNext()) {
            Map block = (Map) iterator.next();
            String opcode = getString(block, "opcode");
            Boolean topLevel = getBoolean(block, "topLevel");
            if (topLevel.booleanValue()) {
                if ("event_whenbroadcastreceived".equals(opcode)) {
                    String name = getString(block, "fields BROADCAST_OPTION<0>");
                    java.write("    public final void " + name + "() {\n");
                    String next = getString(block, "next");
                    while (next != null) {
                        next = compileStatement(java, (Map) blocks.get(next));
                    }
                    java.write("    }\n");
                } else {
                    System.err.println("Unsupported opcode: " + opcode);
                }
            }
        }
    }

    private String compileStatement(PrintWriter java, Map block) {
        String opcode = getString(block, "opcode");
        if ("motion_movesteps".equals(opcode)) {
            Float steps = getFloat(block, "inputs STEPS<1><1>");
            java.write("        Motion.moveSteps("+steps+"f);\n");
        } else if ("motion_turnright".equals(opcode)) {
            Float degrees = getFloat(block, "inputs DEGREES<1><1>");
            java.write("        Motion.turnRight("+degrees+"f);\n");
        } else if ("motion_turnleft".equals(opcode)) {
            Float degrees = getFloat(block, "inputs DEGREES<1><1>");
            java.write("        Motion.turnLeft("+degrees+"f);\n");
        } else if ("control_stop".equals(opcode)) {
            String option = getString(block, "fields STOP_OPTION<0>");
            java.write("        Control.stop("+q(option)+");\n");
        } else {
            System.err.println("Unsupported opcode: " + opcode);
        }
        return getString(block, "next");
    }

    private String q(String s) {
        return s == null ? s : '"' + s + '"';
    }

    private Map getMap(Object object, String path) {
        return (Map) get(object, path);
    }

    private List getList(Object object, String path) {
        return (List) get(object, path);
    }

    private String getString(Object object, String path) {
        Object o = get(object, path);
        return (String) o;
    }

    private Boolean getBoolean(Object object, String path) {
        Object o = get(object, path);
        return o instanceof String ? Boolean.valueOf((String) o) : (Boolean) o;
    }

    private Float getFloat(Object object, String path) {
        Object o = get(object, path);
        return o instanceof String ? Float.valueOf((String) o) : (Float) o;
    }

    private Integer getInteger(Object object, String path) {
        Object o = get(object, path);
        return o instanceof String ? Integer.valueOf((String) o) : (Integer) o;
    }

    private Object get(Object object, String path) {
        if (path == null || path.trim().length() == 0) {
            return object;
        }
        int i = 1;
        while (i < path.length() && path.charAt(i) != '<' && path.charAt(i) != ' ') {
            i++;
            if (path.charAt(i-1)=='>') {
                break;
            }
        }
        String part = path.substring(0, i);
        String rest = path.substring(i < path.length() && path.charAt(i) == ' ' ? i + 1 : i);
        if (object instanceof List && part.length() == 3 && part.charAt(0) == '<' && part.charAt(2) == '>') {
            int index = Integer.parseInt(part.substring(1, part.length()-1));
            List list = (List) object;
            return get(list.get(index), rest);
        } else if (object instanceof Map) {
            Map map = (Map) object;
            return get(map.get(part), rest);
        } else {
            throw new UnsupportedOperationException("path="+path+";part="+part+";rest="+rest);
        }
    }

    protected static final Object OBJECT_END = new Object();
    protected static final Object ARRAY_END = new Object();
    protected static final Object COLON = new Object();
    protected static final Object COMMA = new Object();
    public static final int FIRST = 0;
    public static final int CURRENT = 1;
    public static final int NEXT = 2;

    protected static Map escapes = new HashMap();
    static {
        escapes.put(Character.valueOf('"'), Character.valueOf('"'));
        escapes.put(Character.valueOf('\\'), Character.valueOf('\\'));
        escapes.put(Character.valueOf('/'), Character.valueOf('/'));
        escapes.put(Character.valueOf('b'), Character.valueOf('\b'));
        escapes.put(Character.valueOf('f'), Character.valueOf('\f'));
        escapes.put(Character.valueOf('n'), Character.valueOf('\n'));
        escapes.put(Character.valueOf('r'), Character.valueOf('\r'));
        escapes.put(Character.valueOf('t'), Character.valueOf('\t'));
    }

    protected CharacterIterator it;
    protected char c;
    protected Object token;
    protected StringBuffer buf = new StringBuffer();

    public void reset() {
        it = null;
        c = 0;
        token = null;
        buf.setLength(0);
    }

    protected char next() {
        c = it.next();
        return c;
    }

    protected void skipWhiteSpace() {
        while (Character.isWhitespace(c)) {
            next();
        }
    }

    public Object read(CharacterIterator ci, int start) {
        reset();
        it = ci;
        switch (start) {
            case FIRST:
                c = it.first();
                break;
            case CURRENT:
                c = it.current();
                break;
            case NEXT:
                c = it.next();
                break;
        }
        return read();
    }

    public Object read(CharacterIterator it) {
        return read(it, NEXT);
    }

    public Object read(String string) {
        return read(new StringCharacterIterator(string), FIRST);
    }

    protected Object read() {
        skipWhiteSpace();
        char ch = c;
        next();
        switch (ch) {
            case '"': token = string(); break;
            case '[': token = array(); break;
            case ']': token = ARRAY_END; break;
            case ',': token = COMMA; break;
            case '{': token = object(); break;
            case '}': token = OBJECT_END; break;
            case ':': token = COLON; break;
            case 't':
                next(); next(); next(); // assumed r-u-e
                token = Boolean.TRUE;
                break;
            case'f':
                next(); next(); next(); next(); // assumed a-l-s-e
                token = Boolean.FALSE;
                break;
            case 'n':
                next(); next(); next(); // assumed u-l-l
                token = null;
                break;
            default:
                c = it.previous();
                if (Character.isDigit(c) || c == '-') {
                    token = number();
                }
        }
        // System.out.println("token: " + token); // enable this line to see the token stream
        return token;
    }

    protected Object object() {
        Map ret = new LinkedHashMap();
        Object key = read();
        while (token != OBJECT_END) {
            read(); // should be a colon
            if (token != OBJECT_END) {
                ret.put(key, read());
                if (read() == COMMA) {
                    key = read();
                }
            }
        }

        return ret;
    }

    protected Object array() {
        List ret = new ArrayList();
        Object value = read();
        while (token != ARRAY_END) {
            ret.add(value);
            if (read() == COMMA) {
                value = read();
            }
        }
        return ret;
    }

    protected Object number() {
        int length = 0;
        boolean isFloatingPoint = false;
        buf.setLength(0);

        if (c == '-') {
            add();
        }
        length += addDigits();
        if (c == '.') {
            add();
            length += addDigits();
            isFloatingPoint = true;
        }
        if (c == 'e' || c == 'E') {
            add();
            if (c == '+' || c == '-') {
                add();
            }
            addDigits();
            isFloatingPoint = true;
        }

        String s = buf.toString();
        if (isFloatingPoint) {
            return Float.valueOf(s);
        } else {
            return Integer.valueOf(s);
        }
    }

    protected int addDigits() {
        int ret;
        for (ret = 0; Character.isDigit(c); ++ret) {
            add();
        }
        return ret;
    }

    protected Object string() {
        buf.setLength(0);
        while (c != '"') {
            if (c == '\\') {
                next();
                if (c == 'u') {
                    add(unicode());
                } else {
                    Object value = escapes.get(Character.valueOf(c));
                    if (value != null) {
                        add(((Character) value).charValue());
                    }
                }
            } else {
                add();
            }
        }
        next();

        return buf.toString();
    }

    protected void add(char cc) {
        buf.append(cc);
        next();
    }

    protected void add() {
        add(c);
    }

    protected char unicode() {
        int value = 0;
        for (int i = 0; i < 4; ++i) {
            switch (next()) {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    value = (value << 4) + c - '0';
                    break;
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                    value = (value << 4) + (c - 'a') + 10;
                    break;
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                    value = (value << 4) + (c - 'A') + 10;
                    break;
            }
        }
        return (char) value;
    }
}
