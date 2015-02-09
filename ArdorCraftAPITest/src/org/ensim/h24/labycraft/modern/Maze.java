package org.ensim.h24.labycraft.modern;

import java.util.Stack;

public class Maze {

	private int w,h,wz,hz;
	private byte [] data;

	public final static byte INVALID=-1;
	public final static byte BLANK=2;
	public final static byte WALL=1;
	public final static byte PATH=0;
	public final static byte FREEZONE=3;
	public final static byte FREEZONEDOOR=4;
	

	public Maze(int w, int h)
	{
		//make dims odd
		if(w%2==0) w--;
		if(h%2==0) h--;

		data = new byte[w*h];
		this.w=w;
		this.h=h;
		this.wz = w/3;
		this.hz = h/3;
	}

	public int getWidth() { return w; }
	public int getHeight() { return h; }

	public byte [] generate()
	{
		int sx,sy,ex,ey;

		if(Math.random()>0.5)
		{
			sx=(int)(Math.random()*(double)(w/2))*2+1;
			ex=(int)(Math.random()*(double)(w/2))*2+1;
			if(Math.random()>0.5)
			{
				sy=0;
				ey=h-1;
			}
			else
			{
				sy=h-1;
				ey=0;
			}
		}
		else
		{
			sy=(int)(Math.random()*(double)(h/2))*2+1;
			ey=(int)(Math.random()*(double)(h/2))*2+1;
			if(Math.random()>0.5)
			{
				sx=0;
				ex=w-1;
			}
			else
			{
				sx=w-1;
				ex=0;
			}
		}

		for(int i=0; i<w; i++)
			for(int j=0; j<h; j++)
			{
				if(i==0 || i==w-1 || j==0 || j==h-1) data[i*h+j]=WALL;
				else data[i*h+j]=BLANK;
				if ( i>=wz && i<2*wz && j>=hz && j<2*hz)
					data[i*h+j]=FREEZONE;

			}
		

		data[sx*h+sy]=PATH;
		data[ex*h+ey]=PATH;
		
		
		int px=sx;
		int py=sy;
		
		if(px==0) px++;
		if(px==w-1) px--;
		if(py==0) py++;
		if(py==h-1) py--;
		
		data[px*h+py]=PATH;
		
		int c,r;
		
		Stack<Integer> hx,hy;
		
		hx=new Stack<Integer>();
		hy=new Stack<Integer>();
		
		while(true)
		{			
			c=0;
			if(get(px,py-2)==BLANK) c=c|1;
			if(get(px+2,py)==BLANK) c=c|2;
			if(get(px,py+2)==BLANK) c=c|4;
			if(get(px-1,py)==BLANK) c=c|8;
			
			if(c==0)
			{
				if(hx.empty()) break;
				px=hx.pop();
				py=hy.pop();
				continue;
			}
			
			while(true)
			{
				r=(int)(Math.random()*4.0);
				if((c&(1<<r))!=0)
				{
					c=1<<r;
					break;
				}
			}
			
			hx.push(px);
			hy.push(py);
			
			
			switch(c)
			{
			case 1:
				set(px,py-1,PATH);
				set(px,py-2,PATH);
				setWall(px,py+1);
				setWall(px-1,py+1);
				setWall(px+1,py+1);
				setWall(px-1,py);
				setWall(px+1,py);
				setWall(px-1,py-1);
				setWall(px+1,py-1);
				
				py=py-2;
				
				break;
			case 2:
				set(px+1,py,PATH);
				set(px+2,py,PATH);
				setWall(px-1,py);
				setWall(px-1,py-1);
				setWall(px-1,py+1);
				setWall(px,py-1);
				setWall(px,py+1);
				setWall(px+1,py-1);
				setWall(px+1,py+1);
				
				px=px+2;
				
				break;	
				
			case 4:
				set(px,py+1,PATH);
				set(px,py+2,PATH);
				setWall(px,py-1);
				setWall(px-1,py-1);
				setWall(px+1,py-1);
				setWall(px-1,py);
				setWall(px+1,py);
				setWall(px-1,py+1);
				setWall(px+1,py+1);
				
				py=py+2;
				
				break;
			case 8:
				set(px-1,py,PATH);
				set(px-2,py,PATH);
				setWall(px+1,py);
				setWall(px+1,py-1);
				setWall(px+1,py+1);
				setWall(px,py-1);
				setWall(px,py+1);
				setWall(px-1,py-1);
				setWall(px-1,py+1);
				
				px=px-2;
				
				break;
			}
		}
		
		return data;
	}
	
	private byte get(int x, int y)
	{
		if(x<0 || x>=w || y<0 || y>=h) return INVALID;
		return data[x*h+y];
	}
	
	private void set(int x, int y, byte val)
	{
		if(x<0 || x>=w || y<0 || y>=h) return;
		data[x*h+y]=val;
	}
	
	private void setWall(int x, int y)
	{
		if(x<0 || x>=w || y<0 || y>=h) return;
		if(data[x*h+y]==BLANK) data[x*h+y]=WALL;
	}
	

	public void debug()
	{
		for(int i=0; i<w; i++)
		{
			for(int j=0; j<h; j++)
				System.out.print((int)data[i*h+j]);
			System.out.print("\n");
		}

	}
}
