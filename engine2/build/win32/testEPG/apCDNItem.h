#pragma once
#include <string>

class apCDNItem
{
private:
	apCDNItem(void);
public:
	apCDNItem(const char *sh, const char *st, const char *bh, int ft) {
		m_sh = sh;
		m_st = st;
		m_bh = bh;
		m_ft = ft;
	}

	~apCDNItem(void);

	const char * get_sh() {return m_sh.c_str();}

	const char * get_st() {return m_st.c_str();}

	const char * get_bh() {return m_bh.c_str();}

	int get_ft() {return m_ft;}
private:
	std::string m_sh;
	std::string m_st;
	std::string m_bh;
	int			m_ft;
};

