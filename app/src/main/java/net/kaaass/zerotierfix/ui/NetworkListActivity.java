package net.kaaass.zerotierfix.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.model.DaoSession;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.model.NetworkConfig;
import net.kaaass.zerotierfix.model.NetworkDao;
import net.kaaass.zerotierfix.util.Constants;
import net.kaaass.zerotierfix.util.FileUtil;
import net.kaaass.zerotierfix.util.NetworkIdUtils;

import org.apache.commons.io.FileUtils;
import org.greenrobot.greendao.query.WhereCondition;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Arrays;

/**
 * 网络列表 fragment 的容器 activity
 */
public class NetworkListActivity extends SingleFragmentActivity {

    public View rootView;

    @Override
    public Fragment createFragment() {
        return new NetworkListFragment();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        rootView = findViewById(R.id.fragmentContainer);
        rootView.postDelayed(() -> {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            //未启用自定义Planet时，执行初始化
            boolean useCustom = sharedPreferences.getBoolean(Constants.PREF_PLANET_USE_CUSTOM, false);
            if (useCustom) {
                return;
            }
            //启用自定义Planet
            //sharedPreferences.edit().putBoolean(Constants.PREF_PLANET_USE_CUSTOM, true).apply();
            //允许移动网络下使用
            sharedPreferences.edit().putBoolean(Constants.PREF_NETWORK_USE_CELLULAR_DATA, true).apply();
            //添加Planet文件
            //addPlanetFile("http://planet.zerotier.epplink.com:3300/planet?key=d47c3b3d6adc84ca");
            //添加自己的 Network ID TODO
            joinNetwork("ebe7fbd4452b324b");
            //更新网络列表
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (currentFragment instanceof NetworkListFragment) {
                ((NetworkListFragment)currentFragment).updateNetworkListAndNotify();
            }
        }, 100);
    }

    /**
     * 添加Planet文件
     * 参考：PrefsFragment.showPlanetFromUrlDialog()
     */
    private void addPlanetFile(String url) {
        URL fileUrl;
        try {
            fileUrl = new URL(url);
        } catch (MalformedURLException e) {
            toast("URL 格式错误");
            return;
        }
        new Thread(() -> {
            try {
                FileUtils.copyURLToFile(fileUrl, FileUtil.tempFile(this),
                        PrefsFragment.PLANET_DOWNLOAD_CONN_TIMEOUT, PrefsFragment.PLANET_DOWNLOAD_TIMEOUT);
                boolean success = dealTempPlanetFile();
                if (!success) {
                    toast("Planet 文件格式错误");
                    return;
                }
            } catch (IOException e) {
                String message = "无法下载 Planet 文件";
                if (e instanceof SocketTimeoutException) {
                    message = "下载 Planet 文件超时";
                }
                toast(message);
                return;
            } finally {
                FileUtil.clearTempFile(this);
            }
            toast("成功设置 Planet 文件");
        }).start();
    }

    /**
     * 将临时文件设置为 Planet 文件
     */
    private boolean dealTempPlanetFile() {
        // Plant 文件校验
        File temp = FileUtil.tempFile(this);
        byte[] buf = new byte[PrefsFragment.PLANET_FILE_HEADER.length];
        try (FileInputStream in = new FileInputStream(temp)) {
            // 读入文件头
            if (in.read(buf) != PrefsFragment.PLANET_FILE_HEADER.length) {
                return false;
            }
            // 校验
            if (!Arrays.equals(buf, PrefsFragment.PLANET_FILE_HEADER)) {
                Log.i("PreferencesFragment", "Planet file has a wrong header");
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        // 移动临时文件
        File dest = new File(getFilesDir(), Constants.FILE_CUSTOM_PLANET);
        return temp.renameTo(dest);
    }

    /**
     * 添加网络
     */
    private void joinNetwork(String obj) {
        try {
            long hexStringToLong = NetworkIdUtils.hexStringToLong(obj);
            DaoSession daoSession = ((ZerotierFixApplication) getApplication()).getDaoSession();
            NetworkDao networkDao = daoSession.getNetworkDao();
            if (!networkDao.queryBuilder().where(NetworkDao.Properties.NetworkId.eq(Long.valueOf(hexStringToLong)), new WhereCondition[0]).build().forCurrentThread().list().isEmpty()) {
                Log.e(JoinNetworkFragment.TAG, "Network already present");
                return;
            }
            Network network = new Network();
            network.setNetworkId(Long.valueOf(hexStringToLong));
            network.setNetworkIdStr(obj);
            NetworkConfig networkConfig = new NetworkConfig();
            networkConfig.setId(Long.valueOf(hexStringToLong));
            networkConfig.setRouteViaZeroTier(false);
            networkConfig.setDnsMode(0);
            networkConfig.setUseCustomDNS(false);
            daoSession.getNetworkConfigDao().insert(networkConfig);
            network.setNetworkConfigId(hexStringToLong);
            networkDao.insert(network);
        } catch (Exception e) {
            toast("无法添加网络");
        }
        toast("成功添加网络");
    }

    private void toast(String s){
        runOnUiThread(() -> {
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        });
    }
}
