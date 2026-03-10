package io.github.angelsl.wabbitemu.extract;


import io.github.angelsl.wabbitemu.extract.MsiDatabase.CItem;
import io.github.angelsl.wabbitemu.extract.MsiDatabase.NFatID;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MsiHandler {

    private final MsiDatabase _db;

    public MsiHandler(MsiDatabase database) {
        _db = database;
    }

    public byte[] GetStream(RandomAccessFile file, int index) throws IOException {
        int itemIndex = _db.Refs.get(index).Did;
        final CItem item = _db.Items.get(itemIndex);
        return GetStream(file, item, itemIndex);
    }

    public byte[] GetStream(RandomAccessFile file, CItem item, int itemIndex) throws IOException{
        boolean isLargeStream = (itemIndex == 0 || _db.isLargeStream(item.Size));
        int bsLog = isLargeStream ? _db.SectorSizeBits : _db.MiniSectorSizeBits;

        final long clusterSize = 1L << bsLog;
        final long numClusters64 = (item.Size + clusterSize - 1) >> bsLog;
        if (numClusters64 >= (1L << 31))
            throw new UnsupportedOperationException("Unimplemented");
        final int[] stuff = new int[(int) numClusters64];
        int sid = item.Sid;
        long size = item.Size;
        int i = 0;

        if (size != 0) {
            for (;; size -= clusterSize) {
                if (isLargeStream) {
                    if (sid >= _db.FatSize)
                        throw new IOException("sid >= _db.FatSize");
                    stuff[i++] = sid + 1;
                    sid = _db.Fat.get(sid);
                } else {
                    long val = _db.getMiniCluster(sid);
                    if (sid >= _db.MatSize || val >= (long)1 << 32)
                        throw new IOException("sid >= _db.MatSize || val >= (long)1 << 32");
                    stuff[i++] = (int)val;
                    sid = _db.Mat.get(sid);
                }
                if (size <= clusterSize) {
                    break;
                }
            }
        }

        if (NFatID.fromInt(sid) != NFatID.kEndOfChain) {
            throw new IOException("Not kEndOfChain");
        }

        final int dataSize = (int) item.Size;
        final byte[] fileData = new byte[dataSize];
        final int startIndex = stuff[0] << bsLog;
        file.seek(startIndex);
        file.read(fileData, 0, dataSize);
        return fileData;
    }
}
