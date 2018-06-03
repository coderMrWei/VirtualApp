package com.lody.virtual.client.stub;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.R;
import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.interfaces.IAppRequestListener;
import com.lody.virtual.server.pm.VAppManagerService;

import java.io.File;

/**
 * @Date 18-4-16 15
 * @Author wxd@xdja.com
 * @Descrip:
 */

public class InstallerActivity extends Activity {

    private String TAG = "InstallerActivity";

    LinearLayout ll_install;
    LinearLayout ll_installing;
    LinearLayout ll_installed;
    LinearLayout ll_installed_1;
    LinearLayout ll_check;
    LinearLayout ll_openning;
    TextView tv_warn;
    Button btn_open;
    ImageView img_appicon;
    TextView tv_appname;
    TextView tv_source;

    boolean tv_warn_isshow = false;
    private AppInfo apkinfo;
    private AppInfo sourceapkinfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_installer);
        ll_install = (LinearLayout) findViewById(R.id.ll_install);
        ll_installing = (LinearLayout) findViewById(R.id.ll_installing);
        ll_installed = (LinearLayout) findViewById(R.id.ll_installed);
        ll_installed_1 = (LinearLayout) findViewById(R.id.ll_installed_1);
        ll_check = (LinearLayout) findViewById(R.id.ll_check);
        ll_openning =(LinearLayout) findViewById(R.id.ll_openning);
        tv_warn = (TextView) findViewById(R.id.tv_warn);
        tv_warn.setText("警告：该应用不是来自安全盒应用中心，请注意应用安全。建议在安全盒应用中心下载使用该应用");

        Button btn_install = (Button) findViewById(R.id.btn_install);
        Button btn_quit = (Button) findViewById(R.id.btn_quit);
        btn_open = (Button) findViewById(R.id.btn_open);
        Button btn_cancle = (Button) findViewById(R.id.btn_cancle);
        img_appicon = (ImageView) findViewById(R.id.img_appicon);
        tv_appname = (TextView) findViewById(R.id.tv_appname);
        tv_source = (TextView) findViewById(R.id.tv_source);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        Animation operatingAnim = AnimationUtils.loadAnimation(this, R.anim.imageroate);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        imageView.startAnimation(operatingAnim);
        ImageView ivOpenning = (ImageView) findViewById(R.id.iv_openning);
        ivOpenning.startAnimation(operatingAnim);

        btn_install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.sendEmptyMessage(STATE_INSTALL);
                stateChanged(STATE_INSTALLING);
            }
        });

        btn_quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDelDialog(true);
            }
        });
        btn_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDelDialog(false);
            }
        });

        String path = getIntent().getStringExtra("installer_path");
        String source_apk_packagename = getIntent().getStringExtra("source_apk");
        String source_lable = getIntent().getStringExtra("source_label");
        Log.e("lxf", " Install path : " + path);
        Log.e("lxf", " Install source_apk : " + source_apk_packagename);
        Log.e("lxf", " Install source_lable : " + source_lable);
        if("com.tencent.mm".equals(source_apk_packagename)
                ||"cn.wps.moffice".equals(source_apk_packagename)
                ||"com.android.gallery3d".equals(source_apk_packagename)){

            Intent intent = new Intent();
            intent.setAction("com.xdja.decrypt.COPYFILE");
            intent.putExtra("workspace",VirtualCore.get().getHostPkg());
            intent.putExtra("source_apk", source_apk_packagename);
            intent.putExtra("installer_path", path);
            intent.putExtra("_VA_|_user_id_",0);
            intent.setComponent(new ComponentName("com.xdja.decrypt", "com.xdja.decrypt.DecryptService"));
//            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            VirtualCore.get().getContext().startService(intent);
            stateChanged(STATE_NONE);
        }
        else{
            initView(getIntent());
            stateChanged(STATE_INSTALL);
        }
    }

    private void initView(Intent intent){
        String path = intent.getStringExtra("installer_path");
        String source_apk_packagename = intent.getStringExtra("source_apk");
        String source_lable = intent.getStringExtra("source_label");
        Log.e("lxf", " Install path : " + path);
        Log.e("lxf", " Install source_apk : " + source_apk_packagename);
        Log.e("lxf", " Install source_lable : " + source_lable);

        if(source_lable!=null){
            tv_source.setText("应用来源："+source_lable);
        }else {
            InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(source_apk_packagename, 0);
            sourceapkinfo = parseInstallApk(info.apkPath);
            tv_source.setText("应用来源："+sourceapkinfo.name);
        }
        apkinfo = parseInstallApk(path);
        img_appicon.setImageDrawable(apkinfo.icon);
        tv_appname.setText(apkinfo.name);

        if(InstallerSetting.safeApps.contains(apkinfo.packageName)){
            tv_warn_isshow = false;
        }else{
            tv_warn_isshow = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==100){
                initView(data);
                stateChanged(STATE_INSTALL);
            }
        }
    }

    private void stateChanged(int state){
        switch(state){
            case STATE_NONE:
                ll_openning.setVisibility(View.INVISIBLE);
                ll_check.setVisibility(View.VISIBLE);
                ll_install.setVisibility(View.INVISIBLE);
                ll_installing.setVisibility(View.INVISIBLE);
                ll_installed.setVisibility(View.INVISIBLE);
                ll_installed_1.setVisibility(View.INVISIBLE);
                tv_warn.setVisibility(tv_warn_isshow?View.VISIBLE:View.INVISIBLE);
                break;
            case STATE_INSTALL:
                ll_openning.setVisibility(View.INVISIBLE);
                ll_check.setVisibility(View.INVISIBLE);
                ll_install.setVisibility(View.VISIBLE);
                ll_installing.setVisibility(View.INVISIBLE);
                ll_installed.setVisibility(View.INVISIBLE);
                ll_installed_1.setVisibility(View.INVISIBLE);
                tv_warn.setVisibility(tv_warn_isshow?View.VISIBLE:View.INVISIBLE);
                break;
            case STATE_INSTALLING:
                ll_openning.setVisibility(View.INVISIBLE);
                ll_check.setVisibility(View.INVISIBLE);
                ll_install.setVisibility(View.INVISIBLE);
                ll_installing.setVisibility(View.VISIBLE);
                ll_installed.setVisibility(View.INVISIBLE);
                ll_installed_1.setVisibility(View.INVISIBLE);
                tv_warn.setVisibility(tv_warn_isshow?View.VISIBLE:View.INVISIBLE);
                break;
            case STATE_INSTALLED:
            case STATE_INSTALLFAILED:
                ll_openning.setVisibility(View.INVISIBLE);
                ll_check.setVisibility(View.INVISIBLE);
                ll_install.setVisibility(View.INVISIBLE);
                ll_installing.setVisibility(View.INVISIBLE);
                ll_installed.setVisibility(View.VISIBLE);
                ll_installed_1.setVisibility(View.VISIBLE);
                tv_warn.setVisibility(View.INVISIBLE);
                Intent intent = VirtualCore.get().getLaunchIntent(apkinfo.packageName, VirtualCore.get().myUserId());
                if(intent==null){
                    btn_open.setText("完成");
                }else {
                    btn_open.setText("打开");
                }
                break;
            case STATE_OPENNING:
                ll_openning.setVisibility(View.VISIBLE);
                ll_check.setVisibility(View.INVISIBLE);
                ll_install.setVisibility(View.INVISIBLE);
                ll_installing.setVisibility(View.INVISIBLE);
                ll_installed.setVisibility(View.INVISIBLE);
                ll_installed_1.setVisibility(View.INVISIBLE);
                tv_warn.setVisibility(View.INVISIBLE);
                break;
        }

    }

    private AppInfo parseInstallApk(String path) {
        AppInfo appinfo = new AppInfo();
        File f = new File(path);
        PackageManager pm = VirtualCore.get().getContext().getPackageManager();
        try {
            PackageInfo pkgInfo = VirtualCore.get().getContext().getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), 0);
            ApplicationInfo ai = pkgInfo.applicationInfo;
            ai.sourceDir = f.getAbsolutePath();
            ai.publicSourceDir = f.getAbsolutePath();
            appinfo.packageName = pkgInfo.packageName;
            appinfo.icon = ai.loadIcon(pm);
            appinfo.name = ai.loadLabel(pm);
            appinfo.path = path;
            Log.e(TAG, " packageName : " + appinfo.packageName + " name : " + appinfo.name);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return appinfo;
    }

    public class AppInfo {
        public String packageName;
        public Drawable icon;
        public CharSequence name;
        public String path;
    }
    private final int STATE_NONE= -1;
    private final int STATE_INSTALL = 0;
    private final int STATE_INSTALLING = 1;
    private final int STATE_INSTALLED = 2;
    private final int STATE_INSTALLFAILED = 3;
    private final int STATE_OPENNING = 4;
    private InstallerHandler mHandler = new InstallerHandler();
    class InstallerHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case STATE_INSTALL:

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InstallResult res = VirtualCore.get().installPackage(apkinfo.path, InstallStrategy.UPDATE_IF_EXIST);
                            Message msg1 = new Message();
                            msg1.what = STATE_INSTALLING;
                            msg1.obj = res;
                            mHandler.sendMessage(msg1);
                        }
                    }).start();
                    break;
                case STATE_INSTALLING:
                    InstallResult res = (InstallResult)msg.obj;
                    if (res.isSuccess) {
                        if (res.isUpdate) {
                            VAppManagerService.get().sendUpdateBroadcast(res.packageName, VUserHandle.ALL);
                        } else {
                            VAppManagerService.get().sendInstalledBroadcast(res.packageName, VUserHandle.ALL);
                        }

                        try {
                            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
                            listener.onRequestInstall(apkinfo.packageName);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        //mHandler.sendEmptyMessage(STATE_INSTALLED);
                        stateChanged(STATE_INSTALLED);
                    }else{
                        //mHandler.sendEmptyMessage(STATE_INSTALLFAILED);
                        stateChanged(STATE_INSTALLFAILED);
                    }
                    break;
                case STATE_INSTALLED:
                case STATE_INSTALLFAILED:
                    break;

            }
        }
    }
    private void showDelDialog(final boolean open){

        final AlertDialog delDlg = new AlertDialog.Builder(InstallerActivity.this).create();
        delDlg.getWindow().setGravity(Gravity.BOTTOM);
        delDlg.show();
        delDlg.setContentView(R.layout.custom_installer_del);

        Button btn_del_cancle = delDlg.getWindow().findViewById(R.id.btn_del_cancel);
        btn_del_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delDlg.dismiss();
                if(open){
                    Intent intent = VirtualCore.get().getLaunchIntent(apkinfo.packageName, VirtualCore.get().myUserId());
                    if(intent!=null)
                        VActivityManager.get().startActivity(intent, VirtualCore.get().myUserId());
                }
                finish();
            }
        });
        Button btn_del_del = delDlg.getWindow().findViewById(R.id.btn_del_del);
        btn_del_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File file = new File(apkinfo.path);
                boolean apkexit = file.exists();
                if(apkexit){
                    boolean delsuc = FileUtils.deleteDir(apkinfo.path);
                    Log.e("lxf","Installer delete apk "+ apkinfo.path + " "+delsuc);
                    if(delsuc){
                        showToast(InstallerActivity.this,"安装包删除成功",Toast.LENGTH_SHORT);
                    }else{
                        showToast(InstallerActivity.this,"安装包删除失败",Toast.LENGTH_SHORT);
                    }
                }else{
                    showToast(InstallerActivity.this,"安装包已被删除",Toast.LENGTH_SHORT);
                }

                stateChanged(STATE_OPENNING);
                delDlg.dismiss();
                if(open){
                    Intent intent = VirtualCore.get().getLaunchIntent(apkinfo.packageName, VirtualCore.get().myUserId());
                    if(intent!=null)
                        VActivityManager.get().startActivity(intent, VirtualCore.get().myUserId());
                }
                finish();
            }
        });
    }
    private static void showToast(Context context, String message, int duration) {
        Toast toast = new Toast(context);
        View toastView = LayoutInflater.from(context).inflate(R.layout.toast_install_del, null);
        TextView contentView = toastView.findViewById(R.id.TextViewInfo);
        contentView.setText(message);
        toast.setView(toastView);
        toast.setDuration(duration);
        toast.setGravity(Gravity.BOTTOM, 0,
                context.getResources().getDimensionPixelOffset(R.dimen.dp_110));
        toast.show();
    }

}