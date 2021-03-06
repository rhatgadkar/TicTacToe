#include "read_named_pipe.h"
#include "named_pipe.h"
#include "constants.h"
#include "exceptions.h"
#include <sys/stat.h>  // for mkfifo
#include <fcntl.h>  // for open
#include <string>
#include <cstring>
using namespace std;

ReadNamedPipe::ReadNamedPipe(bool create)
{
	int status;

	if (create)
	{
		status = mkfifo(FIFO_NAME, S_IFIFO | 0666);
		if (status == -1)
			throw runtime_error("ReadNamedPipe::ReadNamedPipe::mkfifo");
	}

	m_fifofd = open(FIFO_NAME, O_RDONLY | O_NDELAY);
	if (m_fifofd == -1)
		throw runtime_error("ReadNamedPipe::ReadNamedPipe::open");
}

string ReadNamedPipe::readPipe(unsigned len) const
{
	int status;
	char buf[MAXBUFLEN];

	memset(buf, 0, MAXBUFLEN);
	status = read(m_fifofd, buf, len);
	if (status == -1)
		throw runtime_error("ReadNamedPipe::read::read");
	string newStr(buf);
	return newStr;
}
