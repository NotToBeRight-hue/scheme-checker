import {describe,it,expect,vi,beforeEach} from 'vitest';
import {api,bootstrapCsrf,ApiError} from './api';
import {exactRuleBody} from './exact-json';
describe('secure API client',()=>{
 beforeEach(()=>{document.cookie='XSRF-TOKEN=; Max-Age=0; path=/';vi.restoreAllMocks()});
 it('bootstraps CSRF and sends header with cookies on mutation',async()=>{const fetcher=vi.fn().mockResolvedValueOnce({ok:true,json:async()=>({csrfToken:'bootstrap'})}).mockResolvedValueOnce({ok:true,status:201,json:async()=>({id:'1'})});vi.stubGlobal('fetch',fetcher);await bootstrapCsrf();document.cookie='XSRF-TOKEN=rotated; path=/';await api('/suggestions','POST',{type:'SCHEME'});const options=fetcher.mock.calls[1][1];expect(options.credentials).toBe('same-origin');expect(options.headers['X-XSRF-TOKEN']).toBe('rotated');expect(options.body).toContain('SCHEME')});
 it('preserves missing-input details and status',async()=>{vi.stubGlobal('fetch',vi.fn().mockResolvedValue({ok:false,status:422,json:async()=>({error:{code:'INPUT_REQUIRED',message:'Complete inputs',details:{missing:['annualIncome']}}})}));try{await api('/profile');expect.fail()}catch(e){expect(e).toBeInstanceOf(ApiError);expect((e as ApiError).details.missing).toEqual(['annualIncome'])}});
 it('handles no-content logout',async()=>{vi.stubGlobal('fetch',vi.fn().mockResolvedValue({ok:true,status:204}));expect(await api('/auth/logout','POST')).toBeNull()});
 it('sends the original large decimal token without a stringify round trip',async()=>{
  const fetcher=vi.fn().mockResolvedValueOnce({ok:true,json:async()=>({csrfToken:'precision'})}).mockResolvedValueOnce({ok:true,status:200,json:async()=>({id:'42'})});
  vi.stubGlobal('fetch',fetcher);await bootstrapCsrf();document.cookie='XSRF-TOKEN=precision; path=/';
  const body=exactRuleBody('{"value":1000000000000000.11}','0');
  await api('/admin/schemes/42/rules','PUT',body);
  expect(fetcher.mock.calls[1][1].body).toBe('{"rule":{"value":1000000000000000.11},"expectedDraftRevision":"0"}');
  expect(fetcher.mock.calls[1][1].headers['X-XSRF-TOKEN']).toBe('precision');
 });
});
