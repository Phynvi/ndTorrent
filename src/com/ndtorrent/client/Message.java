package com.ndtorrent.client;

import java.nio.ByteBuffer;
import java.util.BitSet;

public final class Message {
	static final byte CHOKE = 0;
	static final byte UNCHOKE = 1;
	static final byte INTERESTED = 2;
	static final byte NOT_INTERESTED = 3;
	static final byte HAVE = 4;
	static final byte BITFIELD = 5;
	static final byte REQUEST = 6;
	static final byte PIECE = 7;
	static final byte CANCEL = 8;

	private ByteBuffer data; // <ID><Payload>
	private long timestamp;

	private volatile boolean is_prepared = true;

	private Message(ByteBuffer data) {
		this.data = data;
	}

	public static Message wrap(ByteBuffer data) {
		return new Message(data);
	}

	public void setTimestamp(long t) {
		timestamp = t;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public byte getID() {
		return getLength() == 0 ? -1 : data.get(0);
	}

	public ByteBuffer getData() {
		return data;
	}

	public int getLength() {
		return data.capacity();
	}

	public int getPayloadLength() {
		return Math.max(0, getLength() - 1);
	}

	public int getPieceIndex() {
		if (!isHavePiece() && !isPiece() && !isBlockRequest() && !isCancel())
			throw new UnsupportedOperationException(getType());

		return data.getInt(1);
	}

	public int getBlockBegin() {
		if (!isPiece() && !isBlockRequest() && !isCancel())
			throw new UnsupportedOperationException(getType());

		return data.getInt(5);
	}

	public int getBlockLength() {
		if (!isPiece() && !isBlockRequest() && !isCancel())
			throw new UnsupportedOperationException(getType());

		return isPiece() ? getPayloadLength() - 2 * 4 : data.getInt(9);
	}

	public boolean isPrepared() {
		// Returns True by default.
		return is_prepared;
	}

	public void setPreparedStatus(boolean is_prepared) {
		// Useful for asynchronous message preparation.
		// For example, background threads may be filling the data.
		this.is_prepared = is_prepared;
	}

	public boolean sameBlockRegion(Message other) {
		return getPieceIndex() == other.getPieceIndex()
				&& getBlockBegin() == other.getBlockBegin()
				&& getBlockLength() == other.getBlockLength();
	}

	public boolean isBlockRequest() {
		return getID() == REQUEST;
	}

	public boolean isKeepAlive() {
		return getLength() == 0;
	}

	public boolean isHavePiece() {
		return getID() == HAVE;
	}

	public boolean isPiece() {
		return getID() == PIECE;
	}

	public boolean isCancel() {
		return getID() == CANCEL;
	}

	public boolean isBitfield() {
		return getID() == BITFIELD;
	}

	public boolean isValidBitfield(int nbits_expected) {
		// Spare bits are not checked
		if (!isBitfield())
			return false;
		return (nbits_expected + 7) / 8 == getPayloadLength();
	}

	public BitSet toBitSet() {
		byte[] array = getData().array();
		int length = getPayloadLength() * 8;
		BitSet set = new BitSet(length);
		for (int ofs = 0; ofs < length; ofs++) {
			int i = 1 + ofs / 8; // + 1 to skip ID
			int bit = 7 - ofs % 8;
			set.set(ofs, (array[i] & (1 << bit)) != 0);
		}
		return set;
	}

	public static Message newBitfield(BitSet set, int nbits) {
		byte[] array = new byte[(nbits + 7) / 8 + 1];
		byte b = 0;
		for (int ofs = 0; ofs < nbits; ofs++) {
			b <<= 1;
			b |= set.get(ofs) ? 1 : 0;
			array[1 + ofs / 8] = b;
		}
		int spare_bits = 7 - (nbits + 7) % 8;
		array[array.length - 1] = (byte) (b << spare_bits);
		array[0] = BITFIELD;
		return new Message(ByteBuffer.wrap(array));
	}

	public static Message newCancel(int index, int offset, int length) {
		ByteBuffer data = ByteBuffer.allocate(1 + 3 * 4);
		data.put(CANCEL);
		data.putInt(index);
		data.putInt(offset);
		data.putInt(length);
		return new Message(data);
	}

	public static Message newBlockRequest(int index, int offset, int length) {
		ByteBuffer data = ByteBuffer.allocate(1 + 3 * 4);
		data.put(REQUEST);
		data.putInt(index);
		data.putInt(offset);
		data.putInt(length);
		return new Message(data);
	}

	public static Message newBlock(int index, int offset, int length) {
		// Buffer position will be in the proper place to put the binary data
		// when the method returns.
		ByteBuffer data = ByteBuffer.allocate(1 + 2 * 4 + length);
		data.put(PIECE);
		data.putInt(index);
		data.putInt(offset);
		return new Message(data);
	}

	public static Message newHavePiece(int index) {
		ByteBuffer data = ByteBuffer.allocate(1 + 4);
		data.put(HAVE);
		data.putInt(index);
		return new Message(data);
	}

	public static Message newKeepAlive() {
		return new Message(ByteBuffer.allocate(0));
	}

	public static Message newChoke() {
		return new Message(ByteBuffer.allocate(1).put(CHOKE));
	}

	public static Message newUnchoke() {
		return new Message(ByteBuffer.allocate(1).put(UNCHOKE));
	}

	public static Message newInterested() {
		return new Message(ByteBuffer.allocate(1).put(INTERESTED));
	}

	public static Message newNotInterested() {
		return new Message(ByteBuffer.allocate(1).put(NOT_INTERESTED));
	}

	public boolean isValid() {
		// The caller should double check variable length messages.

		if (isKeepAlive())
			return true;

		switch (getID()) {
		case CHOKE:
		case UNCHOKE:
		case INTERESTED:
		case NOT_INTERESTED:
			return getLength() == 1 + 0;
		case HAVE:
			return getLength() == 1 + 4;
		case BITFIELD:
			return getLength() > 0;
		case REQUEST:
			return getLength() == 1 + 3 * 4;
		case PIECE:
			return getLength() > 2 * 4;
		case CANCEL:
			return getLength() == 1 + 3 * 4;

		default:
			return false;
		}
	}

	public String getType() {
		if (isKeepAlive())
			return "KEEP-ALIVE";

		switch (getID()) {
		case CHOKE:
			return "CHOKE";
		case UNCHOKE:
			return "UNCHOKE";
		case INTERESTED:
			return "INTERESTED";
		case NOT_INTERESTED:
			return "NOT-INTERESTED";
		case HAVE:
			return "HAVE";
		case BITFIELD:
			return "BITFIELD";
		case REQUEST:
			return "REQUEST";
		case PIECE:
			return "PIECE";
		case CANCEL:
			return "CANCEL";

		default:
			return "INVALID";
		}
	}

}
