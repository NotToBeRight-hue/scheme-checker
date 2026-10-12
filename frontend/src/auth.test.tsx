import {it,expect,vi,afterEach} from 'vitest';
import {render,screen,fireEvent,cleanup} from '@testing-library/react';
import {Auth} from './main';
vi.mock('./api',()=>({api:vi.fn(),bootstrapCsrf:vi.fn(),ApiError:class extends Error{}}));
afterEach(cleanup);
it('citizen authentication provides labeled inputs without a repetitive warning',()=>{render(<Auth login={vi.fn()}/>);expect(screen.getByLabelText('Email')).toBeTruthy();expect(screen.getByLabelText('Password')).toBeTruthy();expect(screen.queryByText(/fictional synthetic/)).toBeNull();fireEvent.click(screen.getByRole('button',{name:'Create a citizen account'}));expect(screen.getByLabelText('Full name')).toBeTruthy();expect(screen.getByRole('button',{name:'Create account'})).toBeTruthy()});
