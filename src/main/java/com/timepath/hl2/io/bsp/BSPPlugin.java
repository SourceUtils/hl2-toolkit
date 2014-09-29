package com.timepath.hl2.io.bsp;

import com.timepath.hl2.io.bsp.lump.LumpType;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.provider.ProviderPlugin;
import com.timepath.vfs.provider.zip.ZipFileProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.MetaInfServices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* @author TimePath
*/
@MetaInfServices
public class BSPPlugin implements ProviderPlugin {

    private static final Logger LOG = Logger.getLogger(BSPPlugin.class.getName());

    @Override
    public SimpleVFile.FileHandler register() {
        return new SimpleVFile.FileHandler() {
            @Nullable
            @Override
            public Collection<? extends SimpleVFile> handle(@NotNull final File file) throws IOException {
                if (!file.getName().endsWith(".bsp")) return null;
                final String name = file.getName().replace(".bsp", "");
                return Collections.singleton(new SimpleVFile() {
                    void checkBSP() {
                        if (z != null) return;
                        LOG.log(Level.INFO, "Loading {0}", file);
                        try (@NotNull InputStream is = new FileInputStream(file)) {
                            @Nullable BSP b = BSP.load(is);
                            if (b != null) {
                                z = b.getLump(LumpType.LUMP_PAKFILE);
                            }
                        } catch (IOException e) {
                            LOG.log(Level.SEVERE, null, e);
                        }
                    }

                    @Nullable
                    ZipFileProvider z;

                    @Override
                    public boolean isDirectory() {
                        return true;
                    }

                    @NotNull
                    @Override
                    public String getName() {
                        return name;
                    }

                    @Nullable
                    @Override
                    public InputStream openStream() {
                        return null;
                    }

                    @NotNull
                    @Override
                    public Collection<? extends SimpleVFile> list() {
                        checkBSP();
                        return (z != null) ? z.list() : Collections.<SimpleVFile>emptyList();
                    }

                    @Nullable
                    @Override
                    public SimpleVFile get(@NotNull final String name) {
                        checkBSP();
                        return (z != null) ? z.get(name) : null;
                    }
                });
            }
        };
    }
}
