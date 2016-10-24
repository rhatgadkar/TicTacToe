#ifndef CLIENT_H
#define CLIENT_H

#define SERVERIP "54.183.217.40"
//#define SERVERIP "127.0.0.1"
#define SERVERPORT "4950"
#define MAXBUFLEN 100

#include <netdb.h>

class Client
{
public:
    Client();
    ~Client();
    bool send_position(int pos);
    bool is_p1() { return m_is_p1; }
    bool send_giveup();
    bool send_bye();
    int receive_from_server(char* buf);
private:
    // variables
    bool m_is_p1;
    struct addrinfo* m_p;
    struct addrinfo* m_servinfo;
    int m_sockfd;
    struct timer_params
    {
        int seconds;
        int* got_ack;
    };
    // functions
    static void* timer_countdown(void* parameters);
    static void* receive_thread(void* parameters);
    int create_socket_server(const char* port);
    void handle_syn_ack(char resp[MAXBUFLEN]);  // return port of child server
    int send_to_server(const char* text);
};

#endif  // CLIENT_H