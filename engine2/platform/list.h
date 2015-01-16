/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

#ifndef FF_LIST_H_
#define FF_LIST_H_
#include <stdint.h>
#include <pthread.h>
#if defined(_MSC_VER) || defined(__CYGWIN__)
#include "pperrors.h"
#else
#include "errors.h"
#endif

// has lock protect
class Node
{
friend class List;
private:
	Node() : m_data(NULL), m_pNext(NULL) {}
	Node(void* data) : m_data(data), m_pNext(NULL) {}
	~Node() {}

private:
	void* m_data;
	Node* m_pNext;
};

class List
{
public:
	List();
	~List();
	bool IsEmpty();
	int32_t GetLength();
	bool Insert(int32_t nIndex, void* data); // add by position
	void* Remove(int32_t nIndex); // remove by position
	void Append(void* data); // add to tail
	void* GetAt(int32_t nIndex); // get by position
	void* operator[](int32_t nIndex); // access by position
	void Clear();

private:
	bool IsValidIndex(int32_t nIndex);

	Node* m_pHead;
	Node* m_pTail;
	uint32_t m_nLength;
	pthread_mutex_t	mLock;
};


#endif //FF_LIST_H_

