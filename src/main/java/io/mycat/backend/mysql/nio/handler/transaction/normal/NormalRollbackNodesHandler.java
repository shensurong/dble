/*
 * Copyright (c) 2013, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese 
 * opensource volunteers. you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Any questions about this component can be directed to it's project Web address 
 * https://code.google.com/p/opencloudb/.
 *
 */
package io.mycat.backend.mysql.nio.handler.transaction.normal;


import io.mycat.backend.BackendConnection;
import io.mycat.backend.mysql.nio.MySQLConnection;
import io.mycat.backend.mysql.nio.handler.transaction.AbstractRollbackNodesHandler;
import io.mycat.net.mysql.ErrorPacket;
import io.mycat.server.NonBlockingSession;
import io.mycat.util.StringUtil;

/**
 * @author mycat
 */
public class NormalRollbackNodesHandler extends AbstractRollbackNodesHandler{
	public NormalRollbackNodesHandler(NonBlockingSession session) {
		super(session);
	}

	@Override
	public void resetResponseHandler() {
		responsehandler = NormalRollbackNodesHandler.this;
	}
	@Override
	protected void endPhase(MySQLConnection mysqlCon) {
		// no need, do nothing
	}

	@Override
	protected void rollbackPhase(MySQLConnection mysqlCon) {
		mysqlCon.rollback();
	}

	@Override
	public void okResponse(byte[] ok, BackendConnection conn) {
		if (decrementCountBy(1)) {
			cleanAndFeedback(ok);
		}
	}

	@Override
	public void errorResponse(byte[] err, BackendConnection conn){
		ErrorPacket errPacket = new ErrorPacket();
		errPacket.read(err);
		String errmsg = new String(errPacket.message);
		this.setFail(errmsg);
		conn.quit();
		if (decrementCountBy(1)) {
			cleanAndFeedback(errPacket.toBytes());
		}
	}
	@Override
	public void connectionError(Throwable e, BackendConnection conn){
		LOGGER.warn("backend connect", e);
		String errmsg = new String(StringUtil.encode(e.getMessage(), session.getSource().getCharset()));
		this.setFail(errmsg);
		conn.quit();
		if (decrementCountBy(1)) {
			cleanAndFeedback(errmsg.getBytes());
		}
	}
	@Override
	public void connectionClose(BackendConnection conn, String reason){
		this.setFail(reason);
		if (decrementCountBy(1)) {
			cleanAndFeedback(reason.getBytes());
		}
	}
}