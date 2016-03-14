package com.github.wens.lucene;

import org.apache.lucene.store.*;
import org.apache.lucene.util.Accountable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wens on 16-3-10.
 */
public class LeveldbDirectory extends BaseDirectory implements Accountable {

    private static final int BUFFER_SIZE = 10 * 1024;

    protected final LeveldbFileStore store;

    protected final AtomicLong sizeInBytes = new AtomicLong();

    public LeveldbDirectory(Path path) throws IOException {
        this(path, new SingleInstanceLockFactory());
    }

    /**
     * Sole constructor.
     *
     * @param lockFactory
     */
    protected LeveldbDirectory(Path path, LockFactory lockFactory) throws IOException {
        super(lockFactory);
        store = new LeveldbFileStore(path);
    }


    public LeveldbDirectory(Path path, FSDirectory dir, IOContext context) throws IOException {
        this(path, dir, false, context);
    }

    private LeveldbDirectory(Path path, FSDirectory dir, boolean closeDir, IOContext context) throws IOException {
        this(path);
        for (String file : dir.listAll()) {
            if (!Files.isDirectory(dir.getDirectory().resolve(file))) {
                copyFrom(dir, file, file, context);
            }
        }
        if (closeDir) {
            dir.close();
        }
    }

    @Override
    public final String[] listAll() {
        ensureOpen();
        // NOTE: this returns a "weakly consistent view". Unless we change Dir API, keep this,
        // and do not synchronize or anything stronger. it's great for testing!
        // NOTE: fileMap.keySet().toArray(new String[0]) is broken in non Sun JDKs,
        // and the code below is resilient to map changes during the array population.
        Set<String> keySet = store.listKey();
        List<String> names = new ArrayList<>(keySet.size());
        for (String name : keySet) names.add(name);
        return names.toArray(new String[names.size()]);
    }

    /**
     * Returns the length in bytes of a file in the directory.
     *
     * @throws IOException if the file does not exist
     */
    @Override
    public final long fileLength(String name) throws IOException {
        ensureOpen();
        long size = store.getSize(name);
        if (size == -1) {
            throw new FileNotFoundException(name);
        }
        return size;
    }

    /**
     * Removes an existing file in the directory.
     *
     * @throws IOException if the file does not exist
     */
    @Override
    public void deleteFile(String name) throws IOException {
        ensureOpen();
        long size = store.getSize(name);
        if (size != -1) {
            sizeInBytes.addAndGet(-size);
            store.remove(name);
        } else {
            throw new FileNotFoundException(name);
        }
    }

    /**
     * Creates a new, empty file in the directory with the given name. Returns a stream writing this file.
     */
    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        ensureOpen();

        if (store.contains(name)) {
            store.remove(name);
        }

        return new LeveldbOutputStream(name, store, BUFFER_SIZE, true);
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
    }

    @Override
    public void renameFile(String source, String dest) throws IOException {
        ensureOpen();
        if (!store.contains(source)) {
            throw new FileNotFoundException(source);
        }
        store.move(source, dest);
    }

    /**
     * Returns a stream reading an existing file.
     */
    @Override
    public IndexInput openInput(String name, IOContext context) throws IOException {
        ensureOpen();

        if (!store.contains(name)) {
            throw new FileNotFoundException(name);
        }

        return new LeveldbInputStream(name, store, BUFFER_SIZE);
    }

    /**
     * Closes the store to future operations, releasing associated memory.
     */
    @Override
    public void close() {
        isOpen = false;
        try {
            store.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public long ramBytesUsed() {
        return 0;
    }

    @Override
    public Collection<Accountable> getChildResources() {
        return null;
    }
}
