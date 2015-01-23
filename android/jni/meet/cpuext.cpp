#include "cpuext.h"
#include <string.h>
#include <errno.h>
#include <fcntl.h> // for O_RDONLY

#define TAG "cpufeature_ext"
#include "pplog.h"

static int read_file(const char*  pathname, char*  buffer, size_t  buffsize);

static const char* parse_decimal(const char* input, const char* limit, int* result);


/* Return the frequency of one cpu present on a given device.
 *
 */
int get_cpu_freq(void)
{
    const int len = 64;
	char data[len];
	const char* filename = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";
	int filelen = read_file(filename, data, len);
	if (filelen < 0) {
		LOGE("Could not read %s: %s\n", filename, strerror(errno));
		return 0;
	}
	else
	{
		PPLOGD("file %s len:%d", filename, filelen);
	}

	int cpuFreq = 0;
	if(parse_decimal(data, data+filelen-1, &cpuFreq) == NULL)
	{
		LOGE("parse cpu freq failed");
	}
	return cpuFreq;
}

/* Read the content of /proc/cpuinfo into a user-provided buffer.
 * Return the length of the data, or -1 on error. Does *not*
 * zero-terminate the content. Will not read more
 * than 'buffsize' bytes.
 */
static int
read_file(const char*  pathname, char*  buffer, size_t  buffsize)
{
    int  fd, len;

    fd = open(pathname, O_RDONLY);
    if (fd < 0)
        return -1;

    do {
        len = read(fd, buffer, buffsize);
    } while (len < 0 && errno == EINTR);

    close(fd);

    return len;
}

/* Parse an decimal integer starting from 'input', but not going further
 * than 'limit'. Return the value into '*result'.
 *
 * NOTE: Does not skip over leading spaces, or deal with sign characters.
 * NOTE: Ignores overflows.
 *
 * The function returns NULL in case of error (bad format), or the new
 * position after the decimal number in case of success (which will always
 * be <= 'limit').
 */
static const char*
parse_decimal(const char* input, const char* limit, int* result)
{
    const char* p = input;
    int val = 0;
    while (p < limit) {
        int d = (*p - '0');
        if ((unsigned)d >= 10U)
            break;
        val = val*10 + d;
        p++;
    }
    if (p == input)
        return NULL;

    *result = val;
    return p;
}