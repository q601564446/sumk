/**
 * Copyright (C) 2016 - 2017 youtongluan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yx.rpc.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.yx.log.Log;
import org.yx.rpc.codec.SumkCodecFactory;

public class ServerListener implements Runnable {

	private Logger logger = Log.get(this.getClass());
	private int port;
	private String host = null;
	private IoHandler handler;
	private boolean useExcutor = true;
	private int acceptors = 0;
	private SocketAcceptor acceptor;

	public void setAcceptors(int acceptors) {
		this.acceptors = acceptors;
	}

	public void setUseExcutor(boolean useExcutor) {
		this.useExcutor = useExcutor;
	}

	public ServerListener(String host, int port, List<RequestHandler> handlers) {
		super();
		this.port = port;
		this.host = host;
		this.handler = new ServerHandler(handlers);
	}

	public ServerListener(int port, List<RequestHandler> handlers) {
		super();
		this.port = port;
		this.handler = new ServerHandler(handlers);
	}

	public void run() {
		try {
			acceptor = acceptors > 0 ? new NioSocketAcceptor(acceptors) : new NioSocketAcceptor();
			acceptor.setReuseAddress(true);
			DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();

			chain.addLast("codec", new ProtocolCodecFilter(SumkCodecFactory.factory()));

			if (useExcutor) {

				chain.addLast("exec", new ExecutorFilter(Executors.newCachedThreadPool()));
			}

			acceptor.setHandler(handler);
			acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 120);
			if (SocketSessionConfig.class.isInstance(acceptor.getSessionConfig())) {
				SocketSessionConfig conf = (SocketSessionConfig) acceptor.getSessionConfig();
				conf.setKeepAlive(true);
				conf.setReceiveBufferSize(100);
				conf.setSendBufferSize(8192);
				logger.info("set keep alive");
			}
			InetSocketAddress addr = null;
			if (host == null || host.trim().length() == 0) {
				addr = new InetSocketAddress(port);
			} else {
				addr = new InetSocketAddress(host, port);
			}
			acceptor.bind(addr);
			logger.info("rpc listening on " + addr);

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			System.exit(-1);
		}

	}

	public void stop() throws IOException {
		if (this.acceptor == null) {
			return;
		}
		this.acceptor.dispose(false);
	}

}
