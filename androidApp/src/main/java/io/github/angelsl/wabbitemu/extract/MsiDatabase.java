package io.github.angelsl.wabbitemu.extract;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MsiDatabase {
    private static final byte[] SIGNATURE =
            {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final byte[] kMspSequence = { 0x40, 0x48, (byte) 0x96, 0x45, 0x6C, 0x3E, (byte) 0xE4, 0x45,
            (byte) 0xE6, 0x42, 0x16, 0x42, 0x37, 0x41, 0x27, 0x41, 0x37, 0x41 };
    private static final int kNoDid = 0xFFFFFFFF;
    private static final int kHeaderSize = 512;
    private static final int kNameSizeMax = 64;
    private static final char k_Msi_SpecChar = '!';
    private static final int k_Msi_NumBits = 6;
    private static final int k_Msi_NumChars = 1 << k_Msi_NumBits;
    private static final int k_Msi_CharMask = k_Msi_NumChars - 1;
    private static final int k_Msi_StartUnicodeChar = 0x3800;
    private static final int k_Msi_UnicodeRange = k_Msi_NumChars * (k_Msi_NumChars + 1);
    private static final String k_Msi_Chars =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz._";
    private static final char kCharOpenBracket  = '[';
    private static final char kCharCloseBracket = ']';

    int NumSectorsInMiniStream;
    IntBuffer MiniSids;

    IntBuffer Fat;
    long FatSize;

    IntBuffer Mat;
    int MatSize;

    public List<CItem> Items = new ArrayList<>();
    public List<CRef> Refs = new ArrayList<>();

    int LongStreamMinSize;
    int SectorSizeBits;
    int MiniSectorSizeBits;

    int MainSubfile;

    long PhySize;
    EType Type;

    boolean addNode(int parent, int did) {
        if (did == kNoDid) {
            return true;
        }
        if (did >= Items.size()) {
            return false;
        }
        final CItem item = Items.get(did);
        if (item.isEmpty()) {
            return false;
        }
        final CRef ref = new CRef();
        ref.Parent = parent;
        ref.Did = did;
        final int index = Refs.size();
        Refs.add(ref);
        if (Refs.size() > Items.size()) {
            return false;
        }
        if (!addNode(parent, item.LeftDid)) {
            return false;
        }
        if (!addNode(parent, item.RightDid)) {
            return false;
        }
        if (item.isDir()) {
            if (!addNode(index, item.SonDid)) {
                return false;
            }
        }
        return true;
    }

    private void updatePhySize(long val) {
        if (PhySize < val)
            PhySize = val;
    }

    public boolean isLargeStream(long size) {
        return size >= LongStreamMinSize;
    }

    public long getItemPackSize(long size) {
        long mask = ((long) 1 << (isLargeStream(size) ? SectorSizeBits : MiniSectorSizeBits)) - 1;
        return (size + mask) & ~mask;
    }

    public long getMiniCluster(int sid) throws IOException {
        int subBits = SectorSizeBits - MiniSectorSizeBits;
        int fid = sid >> subBits;
        if (fid >= NumSectorsInMiniStream) {
            throw new IOException("Fid: " + fid + " NumSectorsInMiniStream " + NumSectorsInMiniStream);
        }

        return (((long) MiniSids.get(fid) + 1) << subBits) + (sid & ((1 << subBits) - 1));
    }

    private void readSector(RandomAccessFile inStream, ByteBuffer buf, int sectorSizeBits, long sid) throws IOException {
        updatePhySize((sid + 2) << sectorSizeBits);
        inStream.seek((sid + 1) << sectorSizeBits);
        readStream(inStream, buf, 1 << sectorSizeBits);
    }

    private long readStream(RandomAccessFile inStream, ByteBuffer buf, long processedSize) throws IOException {
        long processedSizeLoc = inStream.read(buf.array(), 0, (int) processedSize);
        if (processedSizeLoc != processedSize) {
            throw new IOException("processedSizeLoc != processedSize " + processedSizeLoc + " " + processedSize);
        }
        return processedSize;
    }

    public void open(RandomAccessFile inStream) throws IOException {
        MainSubfile = -1;
        PhySize = kHeaderSize;

        final ByteBuffer header = ByteBuffer.allocate(kHeaderSize);
        readStream(inStream, header, kHeaderSize);
        final byte[] signature = Arrays.copyOfRange(header.array(), 0, SIGNATURE.length);
        if (!Arrays.equals(signature, SIGNATURE)) {
            throw new IOException("Invalid signature " + Arrays.toString(signature));
        }

        if (Get16(header, 0x1A) > 4) {
            // majorVer
            throw new IOException("Get16(header, 0x1A) = " + Get16(header, 0x1A));
        }
        if (Get16(header, 0x1C) != 0xFFFE) {
            // Little-endian
            throw new IOException("Get16(header, 0x1C) = " + Get16(header, 0x1C));
        }
        int sectorSizeBits = Get16(header, 0x1E);
        boolean mode64bit = (sectorSizeBits >= 12);
        if (mode64bit) {
            throw new IOException("Can't handle 64 bit file");
        }
        int miniSectorSizeBits = Get16(header, 0x20);
        SectorSizeBits = sectorSizeBits;
        MiniSectorSizeBits = miniSectorSizeBits;

        if (sectorSizeBits > 24 ||
                sectorSizeBits < 7 ||
                miniSectorSizeBits > 24 ||
                miniSectorSizeBits < 2 ||
                miniSectorSizeBits > sectorSizeBits)
        {
            throw new IOException("Sector size wrong " + sectorSizeBits + " " + miniSectorSizeBits);
        }
        int numSectorsForFAT = Get32(header, 0x2C); // SAT
        LongStreamMinSize = Get32(header, 0x38);

        int sectSize = 1 << sectorSizeBits;

        ByteBuffer sect = ByteBuffer.allocate(sectSize);

        int ssb2 = sectorSizeBits - 2;
        int numSidsInSec = 1 << ssb2;
        long numFatItems = (long)numSectorsForFAT << ssb2;
        if ((numFatItems >> ssb2) != numSectorsForFAT) {
            throw new IOException("Invalid fat items " + (numFatItems >> ssb2));
        }
        FatSize = numFatItems;

        {
            int numSectorsForBat = Get32(header, 0x48); // master sector allocation table
            final int kNumHeaderBatItems = 109;
            int numBatItems = kNumHeaderBatItems + (numSectorsForBat << ssb2);
            if (numBatItems < kNumHeaderBatItems || ((numBatItems - kNumHeaderBatItems) >> ssb2) != numSectorsForBat)
                throw new IOException("Num bat items invalid " + numBatItems);

            final IntBuffer bat = IntBuffer.allocate(numBatItems);
            int i;
            for (i = 0; i < kNumHeaderBatItems; i++) {
                bat.put(i, Get32(header, 0x4c + i * 4));
            }
            int sid = Get32(header, 0x44);
            for (int s = 0; s < numSectorsForBat; s++) {
                bat.position(i);
                final IntBuffer slice = bat.slice();
                readIDs(inStream, sect, sectorSizeBits, sid, slice);
                i += numSidsInSec - 1;

                sid = bat.get(i);
            }
            numBatItems = i;

            Fat = IntBuffer.allocate((int)numFatItems);
            int j = 0;

            for (i = 0; i < numFatItems; j++, i += numSidsInSec) {
                if (j >= numBatItems) {
                    throw new IOException("j >= numBatItems " + numBatItems);
                }
                Fat.position(i);
                long test = bat.get(j);
                readIDs(inStream, sect, sectorSizeBits, test, Fat.slice());
            }
            FatSize = numFatItems = i;
        }

        int numMatItems;
        {
            int numSectorsForMat = Get32(header, 0x40);
            numMatItems = numSectorsForMat << ssb2;
            if ((numMatItems >> ssb2) != numSectorsForMat)
                throw new IOException("numMatItems >> ssb2 " + (numMatItems >> ssb2));
            Mat = IntBuffer.allocate(numMatItems);
            int i;
            int sid = Get32(header, 0x3C); // short-sector table SID
            for (i = 0; i < numMatItems; i += numSidsInSec) {
                Mat.position(i);
                readIDs(inStream, sect, sectorSizeBits, sid, Mat.slice());
                if (sid >= numFatItems) {
                    throw new IOException("sid >= numFatItems " + sid + " " + numFatItems);
                }
                sid = Fat.get(sid);
            }
            if (NFatID.fromInt(sid) != NFatID.kEndOfChain)
                throw new IOException("NFatId invalid" + sid);
        }

        {
            byte[] used = new byte[(int)numFatItems];
            for (int i = 0; i < numFatItems; i++)
                used[i] = 0;
            int sid = Get32(header, 0x30); // directory inStream SID
            for (; ; ) {
                if (sid >= numFatItems) {
                    throw new IOException("sid >= numFatItems " + sid + " " + numFatItems);
                }
                if (used[sid] != 0) {
                    throw new IOException("used[sid] != 0 " + used[sid]);
                }
                used[sid] = 1;
                readSector(inStream, sect, sectorSizeBits, sid);
                for (int i = 0; i < sectSize; i += 128) {
                    CItem item = new CItem();
                    sect.position(i);
                    item.Parse(sect.slice(), mode64bit);
                    Items.add(item);
                }
                sid = Fat.get(sid);
                if (NFatID.fromInt(sid) == NFatID.kEndOfChain)
                    break;
            }
        }

        final CItem root = Items.get(0);

        {
            int numSectorsInMiniStream;
            {
                long numSatSects64 = (root.Size + sectSize - 1) >> sectorSizeBits;
                if (NFatID.compare(numSatSects64, NFatID.kMaxValue)) {
                    throw new IOException("Invalid numSatSects64 " + numSatSects64);
                }
                numSectorsInMiniStream = (int) numSatSects64;
            }
            NumSectorsInMiniStream = numSectorsInMiniStream;
            MiniSids = IntBuffer.allocate(numSectorsInMiniStream);
            {
                long matSize64 = (root.Size + ((long) 1 << miniSectorSizeBits) - 1) >> miniSectorSizeBits;
                if (NFatID.compare(matSize64, NFatID.kMaxValue)) {
                    throw new IOException("Invalid matSize64 " + matSize64);
                }
                MatSize = (int) matSize64;
                if (numMatItems < MatSize) {
                    throw new IOException("numMatItems < MatSize " + numMatItems + " " + MatSize);
                }
            }

            int sid = root.Sid;
            for (int i = 0; ; i++) {
                if (NFatID.fromInt(sid) == NFatID.kEndOfChain) {
                    if (i != numSectorsInMiniStream) {
                        throw new IOException("i != numSectorsInMiniStream " + i + " " + numSectorsInMiniStream);
                    }
                    break;
                }
                if (i >= numSectorsInMiniStream) {
                    throw new IOException("i >= numSectorsInMiniStream" + i + " " + numSectorsInMiniStream);
                }
                MiniSids.put(i, sid);
                if (sid >= numFatItems) {
                    throw new IOException("sid >= nuMFatItems " + sid + " " + numFatItems);
                }
                sid = Fat.get(sid);
            }
        }

        addNode(-1, root.SonDid);

        int numCabs = 0;

        for (int i = 0; i < Refs.size(); i++) {
            final CItem item = Items.get(Refs.get(i).Did);
            if (item.isDir() || numCabs > 1)
                continue;
            final MsiName msiName = convertName(item.Name);
            final String name = msiName.mName;
            if (msiName.mIsMsiName && !name.isEmpty()) {
                // bool isThereExt = (msiName.Find(L'.') >= 0);
                boolean isMsiSpec = (name.charAt(0) == k_Msi_SpecChar);
                if (name.length() >= 4 && name.substring(name.length() - 4).equalsIgnoreCase(".cab")
                        || !isMsiSpec && name.length() >= 3 && name.substring(name.length() - 3).equalsIgnoreCase("exe")
                    // || !isMsiSpec && !isThereExt
                        )

                {
                    numCabs++;
                    MainSubfile = i;
                }
            }
        }

        if (numCabs > 1) {
            MainSubfile = -1;
        }

        {
            for (int t = 0; t < Items.size(); t++) {
                Update_PhySize_WithItem(t);
            }
        }
        {
            for (int t = 0; t < Items.size(); t++) {
                {
                    final CItem item = Items.get(t);

                    if (isMsiName(item.Name)) {
                        Type = EType.k_Type_Msi;
                        boolean isValid = true;
                        for (int aaa = 0; aaa < kMspSequence.length; aaa++) {
                            final byte val = kMspSequence[aaa];
                            if (val != item.Name.get(aaa)) {
                                isValid = false;
                                break;
                            }
                        }
                        if (isValid) {
                            Type = EType.k_Type_Msp;
                            break;
                        }
                        continue;
                    }
                    if (areEqualNames(item.Name, "WordDocument")) {
                        Type = EType.k_Type_Doc;
                        break;
                    }
                    if (areEqualNames(item.Name, "PowerPoint Document")) {
                        Type = EType.k_Type_Ppt;
                        break;
                    }
                    if (areEqualNames(item.Name, "Workbook")) {
                        Type = EType.k_Type_Xls;
                        break;
                    }
                }
            }
        }
    }

    public String GetItemPath(int index) {
        boolean isEmpty = true;
        final StringBuilder builder = new StringBuilder();
        while (index != kNoDid) {
            final CRef ref = Refs.get(index);
            final CItem item = Items.get(ref.Did);
            if (!isEmpty) {
                builder.insert(0, '/');
                isEmpty = false;
            }
            builder.insert(0, convertName(item.Name));
            index = ref.Parent;
        }
        return builder.toString();
    }

    private static boolean areEqualNames(ByteBuffer rawName, String asciiName)
    {
        for (int i = 0; i < kNameSizeMax / 2; i++)
        {
            int c = Get16(rawName, i * 2);
            int c2 = asciiName.charAt(i);
            if (c != c2)
                return false;
            if (c == 0)
                return true;
        }
        return false;
    }

    private static boolean isMsiName(ByteBuffer p)
    {
        int c = Get16(p, 0);
        return c >= k_Msi_StartUnicodeChar &&
                c <= k_Msi_StartUnicodeChar + k_Msi_UnicodeRange;
    }

    private boolean Update_PhySize_WithItem(int index) {
        final CItem item = Items.get(index);
        boolean isLargeStream = (index == 0 || isLargeStream(item.Size));
        if (!isLargeStream)
            return true;
        int bsLog = SectorSizeBits;
        // streamSpec->Size = item.Size;

        int clusterSize = 1 << bsLog;
        long numClusters64 = (item.Size + clusterSize - 1) >> bsLog;
        if (numClusters64 >= (1 << 31))
            return false;
        int sid = item.Sid;
        long size = item.Size;

        if (size != 0) {
            for (;; size -= clusterSize) {
                // if (isLargeStream)
                {
                    if (sid >= FatSize)
                        return false;
                    updatePhySize(((long)sid + 2) << bsLog);
                    sid = Fat.get(sid);
                }
                if (size <= clusterSize)
                    break;
            }
        }
        return NFatID.fromInt(sid) == NFatID.kEndOfChain;
    }

    private static MsiName convertName(ByteBuffer p) {
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < kNameSizeMax; i += 2) {
            char c = (char) Get16(p, i);
            if (c == 0)
                break;
            s.append(c);
        }

        final String name = s.toString();
        final String msiName = compoundMsiNameToFileName(name);
        if (msiName != null) {
            return new MsiName(msiName, true);
        }
        return new MsiName(CompoundNameToFileName(name), false);
    }

    private static class MsiName {
        public final boolean mIsMsiName;
        public final String mName;

        public MsiName(String name, boolean isMsiName) {
            mIsMsiName = isMsiName;
            mName = name;
        }

        @Override
        public String toString() {
            return String.format("Name: %s, isMsiName: %s", mName, mIsMsiName);
        }
    }

    private static String CompoundNameToFileName(String s)
    {
        final StringBuilder res = new StringBuilder();
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c < 0x20)
            {
                res.append(kCharOpenBracket);
                res.append(Integer.toString(c));
                res.append(kCharCloseBracket);
            }
            else {
                res.append(c);
            }
        }
        return res.toString();
    }


    private static String compoundMsiNameToFileName(String name) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (c < k_Msi_StartUnicodeChar || c > k_Msi_StartUnicodeChar + k_Msi_UnicodeRange)
                return null;
    /*
    if (i == 0)
      res += k_Msi_ID;
    */
            c -= k_Msi_StartUnicodeChar;

            int c0 = (int)c & k_Msi_CharMask;
            int c1 = (int)c >> k_Msi_NumBits;

            if (c1 <= k_Msi_NumChars) {
                builder.append(k_Msi_Chars.charAt(c0));
                if (c1 == k_Msi_NumChars)
                    break;
                builder.append(k_Msi_Chars.charAt(c1));
            } else {
                builder.append(k_Msi_SpecChar);
            }
        }
        return builder.toString();
    }


    private void readIDs(RandomAccessFile inStream, ByteBuffer buf,
            int sectorSizeBits, long sid, IntBuffer dest) throws IOException
    {
        readSector(inStream, buf, sectorSizeBits, sid);
        int sectorSize = 1 << sectorSizeBits;
        for (int t = 0; t < sectorSize; t += 4)
            dest.put(t / 4, Get32(buf, t));
    }

    private static int Get16(ByteBuffer header, int pos) {
        final int val = header.getChar(pos);
        return ((val & 0xFF) << 8) + (val >> 8);
    }

    private static int Get32(ByteBuffer header, int pos) {
        return Get16(header, pos) + (Get16(header, pos + 2) << 16);
    }

    public static class CItem {
        ByteBuffer Name;
        // UInt16 NameSize;
        // int Flags;
        // FILETIME CTime;
        // FILETIME MTime;
        public long Size;
        public int LeftDid;
        public int RightDid;
        public int SonDid;
        public int Sid;
        public NItemType Type;

        public boolean isEmpty() {
            return Type == NItemType.kEmpty;
        }

        public boolean isDir() {
            return Type == NItemType.kStorage || Type == NItemType.kRootStorage;
        }

        public void Parse(ByteBuffer p, boolean mode64bit) {
            final byte[] bytes = new byte[p.capacity()];
            p.get(bytes, p.position(), p.capacity() - p.position());
            Name = ByteBuffer.wrap(bytes);
            Name.position(p.position());
            // NameSize = Get16(p + 64);
            Type = NItemType.fromInt(p.get(66));
            LeftDid = Get32(p, 68);
            RightDid = Get32(p, 72);
            SonDid = Get32(p, 76);
            // Flags = Get32(p + 96);
            //GetFileTimeFromMem(p + 100, &CTime);
            //GetFileTimeFromMem(p + 108, &MTime);
            Sid = Get32(p, 116);
            Size = Get32(p, 120);
            if (mode64bit)
                Size |= ((long)Get32(p, 124) << 32);
        }

        public String getRealName() {
            return convertName(Name).mName;
        }

        @Override
        public String toString() {
            return convertName(Name).mName;
        }
    }

    public static class CRef {
        public int Parent;
        public int Did;
    }

    public enum NItemType {
        kEmpty(0),
        kStorage(1),
        kStream(2),
        kLockBytes(3),
        kProperty(4),
        kRootStorage(5);

        final int mVal;

        NItemType(int val) {
            mVal = val;
        }

        public static NItemType fromInt(int val) {
            for (NItemType itemType : values()) {
                if (itemType.mVal == val) {
                    return itemType;
                }
            }

            return null;
        }
    }

    public enum NFatID {
        kFree(0xFFFFFFFF),
        kEndOfChain(0xFFFFFFFE),
        kFatSector(0xFFFFFFFD),
        kMatSector(0xFFFFFFFC),
        kMaxValue(0xFFFFFFFA);

        final int mVal;

        NFatID(int val) {
            mVal = val;
        }

        public static boolean compare(long val, NFatID id) {
            return val < id.mVal;
        }

        public static NFatID fromInt(int val) {
            for (NFatID itemType : values()) {
                if (itemType.mVal == val) {
                    return itemType;
                }
            }

            return null;
        }
    }

    public enum EType {
        k_Type_Common,
        k_Type_Msi,
        k_Type_Msp,
        k_Type_Doc,
        k_Type_Ppt,
        k_Type_Xls,
    }
}

