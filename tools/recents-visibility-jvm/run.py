"""Exercise the actual RecentTasks hook and protected task-token registry without task mutation."""
import ast,os,subprocess,sys
from pathlib import Path
import sys as _sys, pathlib as _pathlib; _sys.path.insert(0, str(_pathlib.Path(__file__).resolve().parents[1])); from source_paths import package_dir
root=Path(__file__).resolve().parents[2];src=package_dir(root);out=Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
tree=ast.parse((root/'tools/desktop-route-jvm/run.py').read_text(encoding='utf-8-sig'))
files=next(ast.literal_eval(n.value) for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='files' for t in n.targets))
files['android/os/Bundle.java']=files['android/os/Bundle.java'].replace('Map<String,Object>m=', 'public void putBundle(String k,Bundle v){m.put(k,v);}public Bundle getBundle(String k){return (Bundle)m.get(k);}public float getFloat(String k,float d){return m.containsKey(k)?(Float)m.get(k):d;}Map<String,Object>m=')
files['com/jiusis/mistep/NativeRawInputBridge.java']='package com.jiusis.mistep;class NativeRawInputBridge {static void updateTop(int u,float s){}static void updateTop(int u,float s,float x,float y){}}'
files['com/jiusis/mistep/MiStepTopRawInput.java']='package com.jiusis.mistep;class MiStepTopRawInput {static int uid(android.os.Bundle b,ClassLoader l){return -1;}}'
files['com/android/server/wm/Task.java']='package com.android.server.wm;public class Task {}'
files['com/android/server/wm/RecentTasks.java']='package com.android.server.wm;public class RecentTasks {}'
files['de/robv/android/xposed/XC_MethodHook.java']='''package de.robv.android.xposed;import java.util.*;public class XC_MethodHook {
 public class Unhook {public void unhook(){}}
 protected void beforeHookedMethod(MethodHookParam p){}protected void afterHookedMethod(MethodHookParam p){}
 public void before(MethodHookParam p){beforeHookedMethod(p);}public void after(MethodHookParam p){afterHookedMethod(p);}
 public static class MethodHookParam {public Object thisObject,result;public Object[] args;public boolean skip;Map<String,Object> extra=new HashMap<>();
 public void setObjectExtra(String k,Object v){extra.put(k,v);}public Object getObjectExtra(String k){return extra.get(k);}public void setResult(Object r){result=r;skip=true;}}}'''
files['de/robv/android/xposed/XposedBridge.java']='''package de.robv.android.xposed;import java.util.*;public class XposedBridge {
 public static Map<String,XC_MethodHook> hooks=new HashMap<>();public static void log(String s){}}'''
files['de/robv/android/xposed/XposedHelpers.java']=files['de/robv/android/xposed/XposedHelpers.java'].replace('public class XposedHelpers {','''public class XposedHelpers {
 public static XC_MethodHook.Unhook findAndHookMethod(Class<?> c,String n,Object...args){XC_MethodHook h=(XC_MethodHook)args[args.length-1];XposedBridge.hooks.put(n,h);return h.new Unhook();}''')
files['com/jiusis/mistep/RecentsVisibilityRegression.java']='''package com.jiusis.mistep;
import android.os.*;import android.content.*;import java.util.*;import de.robv.android.xposed.*;
public class RecentsVisibilityRegression {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static XC_MethodHook visible,always;
 static XC_MethodHook.MethodHookParam query(Object task){var p=new XC_MethodHook.MethodHookParam();p.args=new Object[]{task};visible.before(p);return p;}
 static boolean top(Object task){var p=new XC_MethodHook.MethodHookParam();p.thisObject=task;always.before(p);return p.skip?((Boolean)p.result):true;}
 static boolean hidden(Object task){var p=query(task);boolean result=p.skip&&Boolean.FALSE.equals(p.result);visible.after(p);return result;}
 static void register(DesktopRouteRegression h,boolean hide,boolean retained){Bundle b=new Bundle();b.putBinder("owner",h.owner);b.putLong("sequence",h.sequence++);b.putBoolean("hideRecents",hide);b.putBoolean("retained",retained);
 b.putParcelableArrayList("rows",new ArrayList<>(List.of(h.row)));b.putParcelableArrayList("recents",new ArrayList<>(List.of(h.row)));
 check(MiStepDesktopLaunchBridge.update(new Intent("state").putExtra("state",b)),"protected registry accepted");}
 static class Notifications {int calls;void notifyTaskStackChanged(){calls++;}}
 static class Atm {Notifications mTaskChangeNotificationController=new Notifications();}
 static class Wms {Atm mAtmService=new Atm();}
 public static void main(String[]args){
 MiStepRecents.install(RecentsVisibilityRegression.class.getClassLoader());visible=XposedBridge.hooks.get("isVisibleRecentTask");always=XposedBridge.hooks.get("isAlwaysOnTopWhenVisible");check(visible!=null&&always!=null,"native hooks installed");
 var h=new DesktopRouteRegression();var p=query(h.task);check(!p.skip&&!top(h.task),"default preserves exact side-task native visibility exception");visible.after(p);check(top(h.task),"exception never leaks outside native recents query");
 register(h,true,false);check(hidden(h.task),"enabled hides exact owned task");check(h.task.mode==6&&h.task.info.taskId==7,"no task mode or identity changed");
 var foreign=new DesktopRouteRegression.Task();check(!hidden(foreign),"same package and ID with different token remains visible");
 h.task.info.userId=999;check(!hidden(h.task),"other user remains native");h.task.info.userId=0;
 h.task.info.displayId=1;check(!hidden(h.task),"other display remains native");h.task.info.displayId=0;
 h.task.mode=1;check(!hidden(h.task),"promoted main is immediately visible");h.task.mode=6;
 h.owner.alive=false;check(!hidden(h.task),"dead Host cannot hide applications");h.owner.alive=true;
 var w=new Wms();MiStepRecents.registryChanged(w);check(w.mAtmService.mTaskChangeNotificationController.calls==1,"hide invalidates native launcher cache");MiStepRecents.registryChanged(w);check(w.mAtmService.mTaskChangeNotificationController.calls==1,"same hidden task set causes no notification storm");
 register(h,true,true);check(!hidden(h.task)&&MiStepDesktopLaunchBridge.hiddenRecentsTasks().isEmpty(),"closed retained workspace restores native recents");MiStepRecents.registryChanged(w);check(w.mAtmService.mTaskChangeNotificationController.calls==2,"exit invalidates hidden cache");
 register(h,false,false);check(!hidden(h.task),"toggle off restores ordinary native filtering");
 p=query(h.task);var nested=query(foreign);check(top(h.task),"foreign nested query cannot borrow outer lease");visible.after(nested);check(!top(h.task),"outer lease restored after nested native query");visible.after(p);check(top(h.task),"outer query restores thread-local state");
 p=query(h.task);register(h,false,false);check(top(h.task),"replaced registry invalidates in-flight lease");visible.after(p);
 register(h,true,false);h.task.info.token=new DesktopRouteRegression.Token();check(!hidden(h.task),"reused task ID cannot hide replacement token");
 System.out.println("Production recents visibility and cache invalidation: "+checks+" checks passed");
 }
}'''
for name,body in files.items():
 p=out/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body,encoding='utf-8')
jdk=Path(os.environ['MISTEP_JAVA_BIN']);production=['MiStepRecents.java','MiStepDesktopLaunchBridge.java','DesktopIconLaunchPolicy.java','MiStepSideRoleChange.java','TaskLaunchIdentity.java','MiStepActions.java']
subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','--release','17','-d',str(out),*[str(out/n) for n in files],*[str(src/n) for n in production]],check=True)
subprocess.run([str(jdk/'java.exe'),'-cp',str(out),'com.jiusis.mistep.RecentsVisibilityRegression'],check=True)
