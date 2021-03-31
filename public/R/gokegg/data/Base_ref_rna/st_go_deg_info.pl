#!usr/bin/perl -w

use strict;

die "usage:perl $0 <deg.list> \n" unless @ARGV == 1;

open A,$ARGV[0];
print "Sample\tGO_enrich_num.\tBP\tCC\tMF\n";
while(<A>){
	chomp;my @a=split;
	open IN,$a[1];
	my $bp=0;	my $cc=0;	my $all=0;	my $mf=0;<IN>;
	while(my $inf=<IN>){
		chomp $inf;my @b=split /\t/,$inf;
		$all++;
		if($b[7] eq "biological_process"){      $bp++;  }
		if($b[7] eq "cellular_component"){      $cc++;  }
		if($b[7] eq "molecular_function"){      $mf++;  }
	}
	close IN;
	print "$a[0]\t$all\t$bp\t$cc\t$mf\n";
}
close A;
