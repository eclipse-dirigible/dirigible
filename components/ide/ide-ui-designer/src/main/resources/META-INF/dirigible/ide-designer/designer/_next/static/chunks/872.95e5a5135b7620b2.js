"use strict";(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[872],{1872:function(e,t,s){s.r(t),t.default=async(e={})=>{try{const t=await window.chooseFileSystemEntries({accepts:[{description:e.description||"",mimeTypes:e.mimeTypes||["*/*"],extensions:e.extensions||[""]}],multiple:e.multiple||!1});if(e.multiple){const e=[];for(const s of t){const t=await s.getFile();t.handle=s,e.push(t)}return e}const s=await t.getFile();return s.handle=t,s}catch(e){throw e}}}}]);