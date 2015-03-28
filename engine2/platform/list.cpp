/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */
#include "list.h"
#define LOG_TAG "List"
#include "log.h"
#include "autolock.h"
#include "loop.h"

List::List() : m_pHead(NULL), m_pTail(NULL), m_nLength(0)
{
	pthread_mutex_init(&mLock, NULL);
}

List::~List()
{
    Clear();
	pthread_mutex_destroy(&mLock);
}

bool List::IsEmpty()
{
	AutoLock autoLock(&mLock);
    return (m_nLength == 0);
}

int32_t List::GetLength()
{
	AutoLock autoLock(&mLock);
    return m_nLength;
}

bool List::Insert(int32_t nIndex, void* data)
{
	AutoLock autoLock(&mLock);

    if (nIndex < 0 || nIndex > (int32_t)m_nLength)
        return false;

    Node* pNewNode = new Node(data);
    if(nIndex == (int32_t)m_nLength)
    {
        m_pTail = pNewNode;
    }
    
    if (nIndex == 0)
    {
        pNewNode->m_pNext = m_pHead;
        m_pHead = pNewNode;
    }
    else
    {
        Node* p = m_pHead;
        for (int32_t i = 1; i < nIndex; i++)
        {
            p = p->m_pNext;
        }
        pNewNode->m_pNext = p->m_pNext;
        p->m_pNext = pNewNode;
    }

    m_nLength++;

    return true;
}

void* List::Remove(int32_t nIndex)
{
    if ( !IsValidIndex(nIndex) )
        return NULL;

	AutoLock autoLock(&mLock);

    Node* prior = m_pHead;
	Node* pTemp = NULL;

    if(nIndex == 0)
    {
		// remove header
		pTemp = m_pHead;
		
		if (m_pTail == m_pHead)
			m_pTail = m_pHead = NULL; // empty list
		else
			m_pHead = m_pHead->m_pNext;
    }
    else
    {
		// move to the Node before "to removed" one
		for (int32_t i = 0; i < nIndex-1; i++)
			prior = prior->m_pNext;

        pTemp = prior->m_pNext; // temp is the one to be deleted
        prior->m_pNext = pTemp->m_pNext;
        if (pTemp == m_pTail)
            m_pTail = prior;// tail is shrink
    }

    void* data = pTemp->m_data;
    delete pTemp;
	pTemp = NULL;
    m_nLength--;
    return data;
}

void List::Append(void* data)
{
	AutoLock autoLock(&mLock);

    Node* pNewNode = new Node(data);
    if ( m_nLength == 0 ) {
        m_pHead = m_pTail = pNewNode;
    }
    else
    {
        m_pTail->m_pNext = pNewNode;
        m_pTail = pNewNode;
    }

    m_nLength++;
}

void* List::GetAt(int32_t nIndex)
{
	if ( !IsValidIndex(nIndex) )
        return NULL;

	AutoLock autoLock(&mLock);

    Node* p = m_pHead;
    for (int32_t i = 0; i < nIndex; i++)
        p = p->m_pNext;

    return p->m_data;
}

void* List::operator[](int32_t nIndex)
{
    return GetAt(nIndex);
}

void List::Clear()
{
	if (m_pHead == NULL)
		return;

	Node* pTemp = NULL;
    while (m_pHead != NULL) {
        pTemp = m_pHead;
        m_pHead = m_pHead->m_pNext;
		delete (Event *)pTemp->m_data;
		pTemp->m_data = NULL;
        delete pTemp;
		pTemp = NULL;
    }

    m_pHead = m_pTail = NULL;
    m_nLength = 0;
}

bool List::IsValidIndex(int32_t nIndex)
{
	AutoLock autoLock(&mLock);
    return (nIndex >= 0 && nIndex < (int)m_nLength);
}