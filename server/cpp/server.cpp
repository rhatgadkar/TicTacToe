#include "server.h"
#include <iostream>
#include <sstream>
#include "server.h"
#include "utilties.h"
#include <string>
using namespace std;

Server::Server(int hostPort)
{
	m_hostPort = hostPort;

	struct addrinfo hints;
	memset(&hints, 0, sizeof(hints));
	hints.ai_family = AF_INET;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_flags = AI_PASSIVE;  // use my IP
	string hostPortStr = intToStr(hostPort);

	int st;
	if ((st = getaddrinfo(NULL, hostPortStr, &hints, &m_servinfo)) != 0)
	{
		cerr << "getaddrinfo: " << gai_strerror(st) << endl;
		throw ServerError();
	}

	// loop through all the results and bind to the first we can
	struct addrinfo *p;
	for (p = m_servinfo; p != NULL; p = p->ai_next)
	{
		if ((m_sockfd = socket(p->ai_family, p->ai_socktype,
				p->ai_protocol)) == -1)
			continue;
		int yes = 1;
		if (setsockopt(m_sockfd, SOL_SOCKET, SO_REUSEADDR, &yes,
				sizeof(int)) == -1)
		{
			cerr << "setsockopt" << endl;
			throw ServerError();
		}
		if (bind(m_sockfd, p->ai_addr, p->ai_addrlen) == -1)
		{
			close(m_sockfd);
			continue;
		}
		break;
	}
	if (p == NULL)
	{
		cerr << "Failed to bind socket." << endl;
		throw ServerError();
	}

	if ((listen(m_sockfd, BACKLOG)) == -1)
	{
		cerr << "listen" << endl;
		throw ServerError();
	}
}

int getHostPort()
{
	return m_hostPort;
}

int getClientPort()
{
	return m_clientPort;
}

string getClientIP()
{
	return m_clientIP;
}

Server::~Server()
{
	close(m_sockfd);
	freeaddrinfo(m_servinfo);
}

string receiveFrom(int time)
{
	fd_set set;
	struct timeval timeout;
	FD_ZERO(&set);
	FD_SET(sockfd, &set);
	timeout.tv_sec = time;
	timeout.tv_usec = 0;

	int numbytes;
	int status;

	char buf[MAXBUFLEN];
	memset(buf, 0, MAXBUFLEN);

	status = select(sockfd + 1, &set, NULL, NULL, &timeout);
	if (status == -1)
	{
		cerr << "select" << endl;
		throw RuntimeError();
	}
	else if (status == 0)
		throw TimeoutError();  // timeout
	else
	{
		for (numbytes = 0; numbytes < MAXBUFLEN; numbytes += status)
		{
			status = recv(sockfd, buf + numbytes,
					MAXBUFLEN - numbytes, 0);
			if (status <= 0)
				break;
		}
		if (status == -1)
		{
			cerr << "read" << endl;
			throw RuntimeError();
		}
		else if (status == 0)
			throw DisconnectError();  // disconnect
		else
		{
			string toReturn(buf);  // read successful
			return toReturn;
		}
	}

}
