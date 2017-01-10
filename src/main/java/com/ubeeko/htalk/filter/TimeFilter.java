package com.ubeeko.htalk.filter;

import java.io.IOException;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.exceptions.DeserializationException;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterBase;
import org.apache.hadoop.hbase.util.Bytes;

public class TimeFilter extends FilterBase {

	Long start = 0L;
	Long end = 0L;

	public TimeFilter(Long start, Long end) {
		this.start = start;
		this.end = end;
	}
	@Override
	public ReturnCode filterKeyValue(Cell cell) throws IOException {
		if (cell.getTimestamp() >= start && cell.getTimestamp() < end) return ReturnCode.INCLUDE;
		else return ReturnCode.NEXT_COL;
	}

	public byte[] toByteArray() {
		byte[] array = new byte[2 * Bytes.SIZEOF_LONG];
		Bytes.putLong(array, 0, start);
		Bytes.putLong(array, Bytes.SIZEOF_LONG, end);
		return array;
	}

	public static Filter parseFrom(final byte[] pbBytes) throws DeserializationException {
		return new TimeFilter(Bytes.toLong(pbBytes), Bytes.toLong(pbBytes, Bytes.SIZEOF_LONG));
	}
}
