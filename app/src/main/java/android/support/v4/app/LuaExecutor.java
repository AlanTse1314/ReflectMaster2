package android.support.v4.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.support.v4.app.reflectmaster.Utils.Utils;
import android.widget.Toast;

import com.luajava.JavaFunction;
import com.luajava.LuaException;
import com.luajava.LuaState;
import com.luajava.LuaStateFactory;

public class LuaExecutor {

    private LuaState L;
    final StringBuilder output = new StringBuilder();

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public LuaExecutor(Context activity, Window jf) {
        L = LuaStateFactory.newLuaState();
        L.openLibs();
        L.pushJavaObject(activity);
        L.setGlobal("this");
        L.pushJavaObject(activity);
        L.setGlobal("activity");
        L.pushJavaObject(jf);
        L.setGlobal("jf");
//        L.pushJavaObject(getAct().getClass().getClassLoader());
//        L.setGlobal("jfclass");
        L.getGlobal("package");
        L.pushString(Environment.getExternalStorageDirectory().toString() + "/ReflectMaster/lua/?.lua");
        L.setField(-2, "path");
//        L.pushString("456");
//        L.setField(-2, "cpath");
        L.pop(1);
        JavaFunction print = new JavaFunction(L) {
            @Override
            public int execute() {
                for (int i = 2; i <= L.getTop(); i++) {
                    int type = L.type(i);
                    String stype = L.typeName(type);
                    String val = null;
                    if (stype.equals("userdata")) {
                        Object obj = L.toJavaObject(i);
                        if (obj != null)
                            val = obj.toString();
                    } else if (stype.equals("boolean")) {
                        val = L.toBoolean(i) ? "true" : "false";
                    } else {
                        val = L.toString(i);
                    }
                    if (val == null)
                        val = stype;
                    output.insert(0, val);
                    output.insert(0, "\t");
                }

                output.insert(0, "\n");
                return 0;
            }
        };
        print.register("print");

    }


    public void executeLua(Context activity, String code) {
        try {
            exeLua(code);
        } catch (LuaException e) {
            output.insert(0, e.toString());
        }
        if (output.length() > 0) {
            new AlertDialog.Builder(activity)
                    .setTitle("运行结果：")
                    .setMessage(output.toString())
                    .setPositiveButton("确定", null)
                    .setNegativeButton("复制", (dialog, which) -> {
                        Utils.writeClipboard(activity, output.toString());
                        Toast.makeText(activity, "已复制到剪切板", Toast.LENGTH_SHORT).show();
                    }).setNeutralButton("清空", (dialog, which) -> output.setLength(0)).show().setCancelable(false);
        }
    }


    private String exeLua(String src) throws LuaException {
        L.setTop(0);
        int ok = L.LloadString(src);
        if (ok == 0) {
            L.getGlobal("debug");
            L.getField(-1, "traceback");
            L.remove(-2);
            L.insert(-2);
            ok = L.pcall(0, 0, -2);
            if (ok == 0) {
                String res = output.toString();
                return res;
            }
        }
        throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
        //return null;

    }


    private String errorReason(int error) {
        switch (error) {
            case 4:
                return "Out of memory";
            case 3:
                return "Syntax error";
            case 2:
                return "Runtime error";
            case 1:
                return "Yield error";
        }
        return "Unknown error " + error;
    }


    public Object doFile(String filePath, Object[] args) {
        int ok = 0;
        try {
            //if (filePath.charAt(0) != '/')
            filePath = Environment.getExternalStorageDirectory().toString() + "/ReflectMaster/lua/" + filePath + ".lua";

            L.setTop(0);
            ok = L.LloadFile(filePath);

            if (ok == 0) {
                L.getGlobal("debug");
                L.getField(-1, "traceback");
                L.remove(-2);
                L.insert(-2);
                int l = args.length;
                for (int i = 0; i < l; i++) {
                    L.pushObjectValue(args[i]);
                }
                ok = L.pcall(l, 1, -2 - l);
                if (ok == 0) {
                    return L.toJavaObject(-1);
                }
            }
            throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
        } catch (LuaException e) {
            output.insert(0, e.toString());
        }

        return null;
    }

}
