from pathlib import Path
import os,subprocess,sys
import sys as _sys, pathlib as _pathlib; _sys.path.insert(0, str(_pathlib.Path(__file__).resolve().parents[1])); from source_paths import package_dir
root=Path(__file__).resolve().parents[2]
out=Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
files={
'de/robv/android/xposed/XC_MethodHook.java':'''package de.robv.android.xposed;public class XC_MethodHook {
public static class Unhook {}public static class MethodHookParam {public Object[] args;}
protected void beforeHookedMethod(MethodHookParam p)throws Throwable{}
public void before(Object...args)throws Throwable{var p=new MethodHookParam();p.args=args;beforeHookedMethod(p);}}''',
'de/robv/android/xposed/XposedBridge.java':'''package de.robv.android.xposed;public class XposedBridge {public static java.util.Map<String,XC_MethodHook> hooks=new java.util.HashMap<>();public static java.util.Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> c,String n,XC_MethodHook h){hooks.put(n,h);return java.util.Set.of(new XC_MethodHook.Unhook());}public static int errors;public static void log(String s){errors++;}}''',
'de/robv/android/xposed/XposedHelpers.java':'''package de.robv.android.xposed;public class XposedHelpers {
public static Class<?> findClass(String n,ClassLoader l){return Object.class;}public static XC_MethodHook hook;public static Object findAndHookMethod(String name,ClassLoader cl,String method,Object...args){if(!name.equals("com.android.server.wm.KeyguardController")||!method.equals("setKeyguardShown")||args[0]!=int.class||args[1]!=boolean.class||args[2]!=boolean.class)throw new AssertionError("wrong native hook");hook=(XC_MethodHook)args[3];return null;}}''',
'io/github/zerohomers0/onesteplite/KeyguardZoomRegression.java':'''package io.github.zerohomers0.onesteplite;import de.robv.android.xposed.*;
public class KeyguardZoomRegression {
static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
public static void main(String[]args)throws Throwable {
var gate=new KeyguardZoom();int[] resets={0};boolean[] nativeVisible={false};
gate.install(KeyguardZoomRegression.class.getClassLoader(),()->{check(gate.blocked(),"barrier must precede reset");check(!nativeVisible[0],"reset must precede native visibility");resets[0]++;});
check(!gate.blocked(),"fresh gate");XposedHelpers.hook.before(4,true,true);check(!gate.blocked()&&resets[0]==0,"secondary display cannot control main geometry");
XposedHelpers.hook.before(0,true,false);nativeVisible[0]=true;check(gate.blocked()&&resets[0]==1,"native lock entry must synchronously reset");
XposedHelpers.hook.before(2,false,false);check(gate.blocked(),"foreign display cannot release lock barrier");
nativeVisible[0]=false;XposedHelpers.hook.before(0,false,true);check(gate.blocked()&&resets[0]==2,"AOD keeps physical geometry");
XposedHelpers.hook.before(0,false,false);check(!gate.blocked()&&resets[0]==2,"native clear removes barrier without opening/restoring a workspace");
gate.install(KeyguardZoomRegression.class.getClassLoader(),()->{throw new IllegalStateException("native reset failed");});
XposedHelpers.hook.before(0,true,false);check(gate.blocked()&&XposedBridge.errors==1,"reset failure cannot open gate or prevent native authentication");
XposedHelpers.hook.before(0,false,false);check(!gate.blocked(),"native unlock remains authoritative after failure");
XposedHelpers.hook.before(0,true,false);check(gate.blocked(),"new lock rearms after prior unlock");
check(!gate.onAuthenticatedReveal(false)&&gate.blocked(),"unauthenticated reveal cannot clear stale controller barrier");
check(gate.onAuthenticatedReveal(true)&&!gate.blocked(),"authenticated native Host handoff releases stale showing barrier");
XposedHelpers.hook.before(0,true,false);check(gate.blocked(),"next native lock rearms after authenticated handoff");
gate.onShown(0,false,false,()->{});int[] early={0};gate.installSleep(KeyguardZoomRegression.class.getClassLoader(),()->early[0]++);
XposedBridge.hooks.get("startedGoingToSleep").before(9,4);check(!gate.sleeping()&&early[0]==0,"secondary display group does not arm early sleep");
XposedBridge.hooks.get("startedGoingToSleep").before(0,4);check(gate.sleeping()&&gate.blocked()&&early[0]==1,"sleep barrier precedes native keyguard notification");
gate.onShown(0,false,false,()->{});check(gate.blocked()&&!gate.onAuthenticatedReveal(true),"interactive tail frames and delayed native clear cannot release sleep barrier");
XposedBridge.hooks.get("startedWakingUp").before(9,1);check(gate.sleeping(),"foreign wake cannot release default sleep");XposedBridge.hooks.get("startedWakingUp").before(0,1);check(!gate.sleeping()&&!gate.blocked(),"default wake releases only early sleep state");
XposedBridge.hooks.get("startedGoingToSleep").before(4);check(gate.sleeping(),"legacy one-argument sleep treats argument as reason");XposedBridge.hooks.get("startedWakingUp").before();check(!gate.sleeping(),"legacy wake is paired");
System.out.println("Production native keyguard barrier: "+checks+" checks passed");}}
'''
}
paths=[]
for name,body in files.items():
 p=out/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body,encoding='utf-8');paths.append(str(p))
paths.append(str(package_dir(root)/'KeyguardZoom.java'))
jdk=Path(os.environ['ONESTEPLITE_JAVA_BIN'])
subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','--release','17','-d',str(out),*paths],check=True)
subprocess.run([str(jdk/'java.exe'),'-cp',str(out),'io.github.zerohomers0.onesteplite.KeyguardZoomRegression'],check=True)
