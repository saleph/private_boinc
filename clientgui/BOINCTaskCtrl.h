// $Id$
//
// The contents of this file are subject to the BOINC Public License
// Version 1.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at
// http://boinc.berkeley.edu/license_1.0.txt
// 
// Software distributed under the License is distributed on an "AS IS"
// basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
// License for the specific language governing rights and limitations
// under the License. 
// 
// The Original Code is the Berkeley Open Infrastructure for Network Computing. 
// 
// The Initial Developer of the Original Code is the SETI@home project.
// Portions created by the SETI@home project are Copyright (C) 2002
// University of California at Berkeley. All Rights Reserved. 
// 
// Contributor(s):
//
// Revision History:
//
// $Log$
// Revision 1.3  2004/09/23 08:28:50  rwalton
// *** empty log message ***
//
// Revision 1.2  2004/09/22 21:53:02  rwalton
// *** empty log message ***
//
// Revision 1.1  2004/09/21 01:26:24  rwalton
// *** empty log message ***
//
//

#ifndef _BOINCTASKCTRL_H_
#define _BOINCTASKCTRL_H_

#if defined(__GNUG__) && !defined(__APPLE__)
#pragma interface "BOINCTaskCtrl.cpp"
#endif


class CBOINCBaseView;

class CBOINCTaskCtrl : public wxHtmlWindow
{
    DECLARE_DYNAMIC_CLASS( CBOINCTaskCtrl )

public:
    CBOINCTaskCtrl();
    CBOINCTaskCtrl( CBOINCBaseView* pView, wxWindowID iHtmlWindowID );

    ~CBOINCTaskCtrl();

    virtual void                BeginTaskPage();
    virtual void                BeginTaskSection(  const wxString& strLink,
                                                   const wxString& strTaskHeaderFilename, 
                                                   bool  bHidden );
    virtual void                CreateTask(        const wxString& strLink,
                                                   const wxString& strTaskIconFilename, 
                                                   const wxString& strTaskName );
    virtual void                EndTaskSection(    bool  bHidden );
    virtual void                UpdateQuickTip(    const wxString& strLink,
                                                   const wxString& strIconFilename,
                                                   const wxString& strTip,
                                                   bool  bHidden );
    virtual void                EndTaskPage();


    virtual void                CreateTaskHeader(  const wxString& strFilename, 
                                                   const wxBitmap& itemTaskBitmap, 
                                                   const wxString& strTaskName ); 


    virtual void                AddVirtualFile(    const wxString& strFilename, 
                                                   wxImage& itemImage, 
                                                   long lType );
    virtual void                AddVirtualFile(    const wxString& strFilename, 
                                                   const wxBitmap& itemBitmap, 
                                                   long lType );
    virtual void                RemoveVirtualFile( const wxString& strFilename );


    virtual void                OnRender( wxTimerEvent& event );
    virtual bool                OnSaveState( wxConfigBase* pConfig );
    virtual bool                OnRestoreState( wxConfigBase* pConfig );

    virtual void                OnLinkClicked( const wxHtmlLinkInfo& link );
    virtual void                OnCellMouseHover( wxHtmlCell* cell, wxCoord x, wxCoord y );
    virtual wxHtmlOpeningStatus OnOpeningURL( wxHtmlURLType type, const wxString& url, wxString *redirect );

private:
    
    template < class T >
        void                FireOnLinkClickedEvent( T pView, const wxHtmlLinkInfo& link );

    template < class T >
        void                FireOnCellMouseHoverEvent( T pView, wxHtmlCell* cell, wxCoord x, wxCoord y );

    CBOINCBaseView*         m_pParentView;

    wxString                m_strTaskPage;

};


#endif

