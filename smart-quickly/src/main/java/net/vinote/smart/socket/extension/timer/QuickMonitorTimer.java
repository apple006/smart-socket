package net.vinote.smart.socket.extension.timer;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.logger.RunLogger;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.service.filter.SmartFilter;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.transport.TransportSession;

/**
 * 服务器监测定时器
 *
 * @author Seer
 * @version QuickMonitorTimer.java, v 0.1 2015年3月18日 下午11:25:21 Seer Exp.
 */
public class QuickMonitorTimer extends QuickTimerTask implements SmartFilter {
	/** 当前周期内消息 流量监控 */
	private AtomicLong flow = new AtomicLong(0);
	/** 当前周期内接受消息数 */
	private AtomicInteger recMsgnum = new AtomicInteger(0);

	/** 当前周期内丢弃消息数 */
	private AtomicInteger discardNum = new AtomicInteger(0);

	/** 当前周期内处理消息数 */
	private AtomicInteger processMsgNum = new AtomicInteger(0);

	/** 当前积压待处理的消息数 */
	private AtomicInteger messageStorage = new AtomicInteger(0);

	private volatile long totleProcessMsgNum = 0;

	@Override
	public void filterDataEntrys(TransportSession session, List<DataEntry> d) {
	}

	@Override
	protected long getDelay() {
		return getPeriod();
	}

	@Override
	protected long getPeriod() {
		return TimeUnit.MINUTES.toMillis(1);
	}

	public void processFilter(Session session, DataEntry d) {
		processMsgNum.incrementAndGet();
		messageStorage.decrementAndGet();
		totleProcessMsgNum++;
	}

	public void readFilter(TransportSession session, DataEntry d) {
		flow.addAndGet(d.getData().length);
		recMsgnum.incrementAndGet();
		messageStorage.incrementAndGet();
	}

	public void receiveFailHandler(TransportSession session, DataEntry d) {
		discardNum.incrementAndGet();
		messageStorage.decrementAndGet();
		RunLogger.getLogger().log(Level.FINEST,
				"HexData -->" + StringUtils.toHexString(d.getData()));
	}

	@Override
	public void run() {
		long curFlow = this.flow.getAndSet(0);
		int curRecMsgnum = this.recMsgnum.getAndSet(0);
		int curDiscardNum = this.discardNum.getAndSet(0);
		int curProcessMsgNum = this.processMsgNum.getAndSet(0);
		RunLogger.getLogger().log(
				Level.SEVERE,
				"\r\nFlow of Message:\t" + curFlow * 1.0 / (1024 * 1024)
						+ "(MB)" + "\r\nNumber of Message:\t" + curRecMsgnum
						+ "\r\nAvg Size of Message:\t"
						+ (curRecMsgnum > 0 ? curFlow * 1.0 / curRecMsgnum : 0)
						+ "\r\nNumber of Discard:\t" + curDiscardNum
						+ "\r\nNum of Process Msg:\t" + curProcessMsgNum
						+ "\r\nStorage of Message:\t" + messageStorage.get()
						+ "\r\nTotal Num of Process Msg:\t"
						+ totleProcessMsgNum);
	}

	@Override
	public void writeFilter(TransportSession session, ByteBuffer d) {
	}

}