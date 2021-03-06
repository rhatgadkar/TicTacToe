import argparse
from screenutils import Screen, list_screens
import table_parser as tp
from time import sleep
import os
import re
import traceback
import commands


WORKING_DIR = ''
IDENTITY_FILE = ''
STARTING_USER_NUM = ''
DEPLOY_SCREENS_CMD = 'python run_java_test.py'
PSQL_PREFIX_CMD = "psql -d mydb -c "
PSQL_SELECT_CMD = "'" + PSQL_PREFIX_CMD + '"SELECT * FROM tttrecords;"' + "'"
PSQL_DELETE_RECORDS_CMD = "'" + PSQL_PREFIX_CMD + \
        '"DELETE FROM tttrecords;"' + "'"
PSQL_DELETE_LOGIN_CMD = "'" + PSQL_PREFIX_CMD + '"DELETE FROM tttlogin;"' + "'"
PS_CMD = "ps ww"
SERVER_ADDR = "ubuntu@54.219.156.253"


def get_raw_psql_table_output():
    """
    Gets the raw psql output of the tttrecords table.
    """
    ssh_cmd = 'ssh -i ' + IDENTITY_FILE + ' ' + SERVER_ADDR
    get_tttrecords_cmd = ssh_cmd + ' ' + PSQL_SELECT_CMD
    raw_data = commands.getoutput(get_tttrecords_cmd)
    return raw_data


def deploy_screens(num_screens, move_str, collect_logs=True):
    """
    Deploy the screen session's for each client using the run_java_test.py
    script with provided arguments of the working directory and move set.
    """
    for i in range(STARTING_USER_NUM, num_screens + STARTING_USER_NUM):
        scr_name = 's' + str(i)
        fvt_args = WORKING_DIR + ' ' + scr_name + ' ' + move_str
        s = Screen(scr_name, True)
	if collect_logs:
            s.enable_logs(scr_name + '.log')
        s.send_commands(DEPLOY_SCREENS_CMD + ' ' + fvt_args)


def kill_screens():
    """
    Destroy all running screen sessions. This also terminates the corresponding
    clients.
    """
    screen_name_regex = r'^s[1-9][0-9]*'
    screen_name_prog = re.compile(screen_name_regex)
    for s in list_screens():
        if screen_name_prog.match(s.name):
            s.kill()


def get_file_wins_losses(screen_log):
    """
    Get wins and losses from screen session's log file.
    """
    f = None
    try:
        f = open(screen_log, 'r')
    except:
        raise Exception('%s cannot be opened for reading.' % (screen_log))
    win_regex = r'.*You win.*'
    loss_regex = r'.*You lose.*'
    win_prog = re.compile(win_regex)
    loss_prog = re.compile(loss_regex)
    file_wins = 0
    file_losses = 0
    for line in f:
        if win_prog.match(line):
            file_wins += 1
        elif loss_prog.match(line):
            file_losses += 1
    f.close()
    return (file_wins, file_losses)


def verify_wins_losses():
    """
    Verify the wins and loss count from the screen session logs and psql table
    output.

    Example psql_table_data:
    [['s1', '3', '3'], ['s2', '3', '3'], ['s3', '3', '3']]
    """
    raw_psql_table = get_raw_psql_table_output()
    (psql_table_headers, psql_table_data) = \
            tp.get_psql_table_data(raw_psql_table)
    for data_list in psql_table_data:
        username = data_list[0]
        table_wins = int(data_list[1])
        table_losses = int(data_list[2])
        screen_log = os.getcwd() + '/' + username + '.log'
        (file_wins, file_losses) = (0, 0)
        try:
            (file_wins, file_losses) = get_file_wins_losses(screen_log)
        except:
            # screen log does not exist for the user
            continue
        if file_wins == table_wins - 1 or file_wins == table_wins + 1 or \
                file_wins == table_wins:
            if file_losses == table_losses - 1 or \
                    file_losses == table_losses + 1 or \
                    file_losses == table_losses:
                        continue
        print 'File and table wins/losses are not within +/-1 for ' + username
        print 'file wins: ' + str(file_wins)
        print 'file losses: ' + str(file_losses)
        print 'table wins: ' + str(table_wins)
        print 'table losses: ' + str(table_losses)
        return False
    return True


def cleanup():
    """
    Delete screen session logs and clear both psql records and login tables.
    """
    # delete screen logs
    screen_log_regex = r'^s[1-9][0-9]*\.log'
    screen_log_prog = re.compile(screen_log_regex)
    dir_items = os.listdir(os.getcwd())
    for item in dir_items:
        if screen_log_prog.match(item):
            commands.getoutput('rm ' + os.getcwd() + '/' + item)
    # delete psql table entries
    ssh_cmd = 'ssh -i ' + IDENTITY_FILE + ' ' + SERVER_ADDR + ' ' + \
            PSQL_DELETE_RECORDS_CMD
    commands.getoutput(ssh_cmd)
    ssh_cmd = 'ssh -i ' + IDENTITY_FILE + ' ' + SERVER_ADDR + ' ' + \
            PSQL_DELETE_LOGIN_CMD
    commands.getoutput(ssh_cmd)


def get_ports_usage():
    """
    Get the usage of ports from screen logs.
    """
    ports_usage = {}
    starting_port = 4951
    ending_port = 5950
    for port in range(starting_port, ending_port + 1):
        ports_usage[port] = False
        port_regex = '.*' + str(port) + '.*'
        port_prog = re.compile(port_regex)
        # verify if the port_regex is matched in the log files
        screen_log_regex = r'^s[1-9][0-9]*\.log'
        screen_log_prog = re.compile(screen_log_regex)
        dir_items = os.listdir(os.getcwd())
        for item in dir_items:
            found_port = False
            if screen_log_prog.match(item):
                f = None
                try:
                    f = open(item, 'r')
                    for line in f:
                        if port_prog.match(line):
                            ports_usage[port] = True
                            found_port = True
                            break
                except:
                    raise Exception('%s cannot be opened for reading.' %
                            (item))
                f.close()
                if found_port:
                    break
    for port in sorted(ports_usage):
        print str(port) + ': ' + str(ports_usage[port])


# tests:
def basic_test():
    """
    5 clients are running for 10 minutes with move set of '1 2 3 4 5 6 7 8 9'.
    """
    result = False
    move_str = '1 2 3 4 5 6 7 8 9'
    num_screens = 5
    try:
        deploy_screens(num_screens, move_str)
        # wait 10 minutes before exit
        sleep(60 * 10)
        kill_screens()
        # verify results of screen logs with psql table output
        result = verify_wins_losses()
        if not result:
            return False
    except:
        print 'Exception occurred.'
        traceback.print_exc()
        return False
    get_ports_usage()
    cleanup()
    return result


def p1_giveup_test():
    """
    5 clients are running for 10 minutes with move set of '1 2'. So P1 will
    timeout and thus giveup.
    """
    result = False
    move_str = '1 2'
    num_screens = 5
    try:
        deploy_screens(num_screens, move_str)
        # wait 10 minutes before exit
        sleep(60 * 10)
        kill_screens()
        # verify results of screen logs with psql table output
        result = verify_wins_losses()
        if not result:
            return False
    except:
        print 'Exception occurred.'
        traceback.print_exc()
        return False
    get_ports_usage()
    cleanup()
    return result


def p2_giveup_test():
    """
    5 clients are running for 10 minutes with move set of '1'. So P2 will
    timeout and thus giveup.
    """
    result = False
    move_str = '1'
    num_screens = 5
    try:
        deploy_screens(num_screens, move_str)
        # wait 10 minutes before exit
        sleep(60 * 10)
        kill_screens()
        # verify results of screen logs with psql table output
        result = verify_wins_losses()
        if not result:
            return False
    except:
        print 'Exception occurred.'
        traceback.print_exc()
        return False
    get_ports_usage()
    cleanup()
    return result


def basic_21_clients_test():
    """
    21 clients are running for 30 minutes with move set of '1 2 3 4 5 6 7 8 9'.
    """
    result = False
    move_str = '1 2 3 4 5 6 7 8 9'
    num_screens = 21
    try:
        deploy_screens(num_screens, move_str)
        # wait 30 minutes before exit
        sleep(60 * 30)
        kill_screens()
        # verify results of screen logs with psql table output
        result = verify_wins_losses()
        if not result:
            return False
    except:
        print 'Exception occurred.'
        traceback.print_exc()
        return False
    get_ports_usage()
    cleanup()
    return result


def main(args):
    """
    This is the entry point for running the functional tests.

    arg 1: Java client working directory
    arg 2: SSH private key file
    """
    global WORKING_DIR
    global IDENTITY_FILE
    global STARTING_USER_NUM
    WORKING_DIR = args.working_dir
    IDENTITY_FILE = args.identity_file
    STARTING_USER_NUM = args.start_user_num
    result = basic_test()
    if not result:
        print 'basic_test failed.'
        return
    result = p1_giveup_test()
    if not result:
        print 'p1_giveup_test failed.'
        return
    result = p2_giveup_test()
    if not result:
        print 'p2_giveup_test failed.'
        return
    result = basic_21_clients_test()
    if not result:
        print 'basic_21_clients_test failed.'
        return
    print 'All tests passed.'


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("working_dir", help="Java client working directory",
                        type=str)
    parser.add_argument("identity_file",
                        help="SSH private key (identity) file", type=str)
    # if start_user_num is '1', it will begin deploying screens from 's1'
    parser.add_argument("start_user_num", help="The starting num of the user",
                        type=int)
    args = parser.parse_args()
    main(args)
